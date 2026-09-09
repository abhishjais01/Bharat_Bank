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

/**
 * Takes the log off the caller's thread.
 *
 * <p>This is the single most important safety property in the platform. A
 * balance enquiry made by a customer must not wait on an HTTP round trip to
 * logging-api, and must not fail if that service is down. Before this class
 * existed the send happened inline, so every logged line added network latency
 * to a customer-facing request.
 *
 * <p>The queue is bounded and the overflow policy is drop-oldest. Both are
 * deliberate:
 * <ul>
 *   <li>Unbounded would convert a logging-api outage into an
 *       OutOfMemoryError in the banking service - the observability platform
 *       taking down the thing it observes.</li>
 *   <li>Blocking on a full queue would reintroduce exactly the latency this
 *       class exists to remove.</li>
 *   <li>Dropping the <em>oldest</em> keeps the newest lines, which are the
 *       ones an engineer is looking at during an incident.</li>
 * </ul>
 * Every drop increments a counter, so silent loss is still visible loss.
 */
public class AsyncLogSink implements LogSink, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(AsyncLogSink.class);

    private final LogSink delegate;
    private final ObservabilityProperties.Async config;
    // Holds both payload kinds; the worker dispatches on type. One queue
    // rather than two keeps the bound on total memory honest.
    private final BlockingQueue<Object> queue;
    private final ExecutorService workers;
    private final AtomicBoolean running = new AtomicBoolean(true);

    private final Counter submitted;
    private final Counter dropped;

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

    @Override
    public void send(LogIngestRequest request) {
        enqueue(request);
    }

    @Override
    public void sendAudit(AuditIngestRequest request) {
        enqueue(request);
    }

    private void enqueue(Object request) {

        SinkMetrics.increment(submitted);

        if (!running.get()) {
            return;
        }

        if (queue.offer(request)) {
            return;
        }

        // Full. Make room by discarding the oldest, then take the slot.
        queue.poll();
        SinkMetrics.increment(dropped);

        if (!queue.offer(request)) {
            // Another worker raced us for the slot. Drop this one rather than
            // retry - the caller is a customer request and owes us nothing.
            SinkMetrics.increment(dropped);
        }
    }

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
                // The delegate already handles its own failures; this is the
                // last line of defence so one bad log cannot kill the worker.
                log.warn("Log sink worker recovered from an error : {}", ex.getMessage());
            }
        }
    }

    /**
     * Gives in-flight logs a chance to reach logging-api on shutdown, rather
     * than losing whatever was queued when the pod was told to stop.
     */
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

    /** Visible for tests. */
    public int queued() {
        return queue.size();
    }

    private static ThreadFactory threadFactory() {

        AtomicInteger counter = new AtomicInteger();

        return runnable -> {
            Thread thread = new Thread(runnable,
                    "observability-log-sink-" + counter.incrementAndGet());
            // Daemon: a stuck log worker must never hold up JVM shutdown.
            thread.setDaemon(true);
            return thread;
        };
    }
}
