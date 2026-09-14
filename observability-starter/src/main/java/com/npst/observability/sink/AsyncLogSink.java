package com.npst.observability.sink;

import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.contract.AuditIngestRequest;
import com.npst.observability.contract.LogIngestRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// sends records from a background thread so requests never wait on logging-api
public class AsyncLogSink implements LogSink, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AsyncLogSink.class);

    private final LogSink delegate;
    private final ObservabilityProperties.Async config;
    private final BlockingQueue<Object> queue;
    private final ExecutorService workers;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Counter submitted;
    private final Counter dropped;

    // create the bounded queue and start the worker threads
    public AsyncLogSink(LogSink delegate,
                        ObservabilityProperties.Async config,
                        MeterRegistry meterRegistry) {

        this.delegate = delegate;
        this.config = config;
        this.queue = new ArrayBlockingQueue<>(config.getQueueCapacity());

        this.submitted = SinkMetrics.counter(meterRegistry, "submitted",
                "Logs handed to the async sink");
        this.dropped = SinkMetrics.counter(meterRegistry, "dropped",
                "Logs discarded because the queue was full");

        this.workers = Executors.newFixedThreadPool(config.getWorkers(), threadFactory());

        for (int i = 0; i < config.getWorkers(); i++) {
            workers.submit(this::drain);
        }
    }

    // logs and audit records go into the same queue
    @Override
    public void send(LogIngestRequest request) {
        enqueue(request);
    }

    @Override
    public void sendAudit(AuditIngestRequest request) {
        enqueue(request);
    }

    // add to the queue without blocking
    private void enqueue(Object request) {

        SinkMetrics.increment(submitted);

        if (!running.get()) {
            return;
        }

        // normal case: there is room
        if (queue.offer(request)) {
            return;
        }

        // queue is full: drop the oldest entry instead of blocking the request thread
        queue.poll();
        SinkMetrics.increment(dropped);

        if (!queue.offer(request)) {
            SinkMetrics.increment(dropped);
        }
    }

    // worker loop: take items from the queue and send them
    private void drain() {

        while (running.get() || !queue.isEmpty()) {
            try {
                Object item = queue.poll(200, TimeUnit.MILLISECONDS);

                switch (item) {
                    case LogIngestRequest request -> delegate.send(request);
                    case AuditIngestRequest request -> delegate.sendAudit(request);
                    case null -> { }
                    default -> log.warn("Unknown payload on the log queue : {}",
                            item.getClass().getName());
                }

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;

            } catch (Exception ex) {
                log.warn("Log sink worker recovered from an error : {}", ex.getMessage());
            }
        }
    }

    // on shutdown, give the queue a few seconds to empty
    @Override
    public void destroy() throws InterruptedException {

        running.set(false);
        workers.shutdown();

        if (!workers.awaitTermination(config.getShutdownTimeout().toMillis(),
                TimeUnit.MILLISECONDS)) {

            log.warn("Log sink did not drain within {} - {} log(s) lost",
                    config.getShutdownTimeout(), queue.size());

            workers.shutdownNow();
        }
    }

    // current queue size, used in tests
    public int queued() {
        return queue.size();
    }

    // named daemon threads for the workers
    private static ThreadFactory threadFactory() {

        AtomicInteger counter = new AtomicInteger();

        return runnable -> {
            Thread thread = new Thread(runnable,
                    "observability-log-sink-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
