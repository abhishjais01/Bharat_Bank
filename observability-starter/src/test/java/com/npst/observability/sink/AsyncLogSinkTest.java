package com.npst.observability.sink;

import com.npst.observability.config.ObservabilityProperties;
import com.npst.observability.contract.LogIngestRequest;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The safety properties that let this platform run in front of a bank:
 * logging never blocks a customer request, never throws at the caller, and
 * never grows without bound when logging-api is down.
 */
class AsyncLogSinkTest {

    @Test
    void deliversWithoutBlockingTheCaller() throws Exception {

        CountDownLatch delivered = new CountDownLatch(3);
        List<LogIngestRequest> received = new CopyOnWriteArrayList<>();

        LogSink downstream = request -> {
            received.add(request);
            delivered.countDown();
        };

        AsyncLogSink sink = new AsyncLogSink(downstream, asyncConfig(100),
                new SimpleMeterRegistry());

        sink.send(request("one"));
        sink.send(request("two"));
        sink.send(request("three"));

        assertThat(delivered.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(received).hasSize(3);

        sink.destroy();
    }

    @Test
    void aSlowDownstreamDoesNotSlowTheCaller() throws Exception {

        // Downstream takes 200ms per log - roughly a struggling logging-api.
        LogSink slow = request -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        AsyncLogSink sink = new AsyncLogSink(slow, asyncConfig(100),
                new SimpleMeterRegistry());

        long startedAt = System.currentTimeMillis();

        for (int i = 0; i < 20; i++) {
            sink.send(request("log-" + i));
        }

        long elapsed = System.currentTimeMillis() - startedAt;

        assertThat(elapsed)
                .as("20 logs against a 200ms downstream would be 4s inline; "
                        + "the caller must return immediately")
                .isLessThan(1_000);

        sink.destroy();
    }

    @Test
    void dropsOldestRatherThanGrowingWithoutBound() throws Exception {

        // Downstream that never completes, so the queue fills and stays full.
        CountDownLatch blocked = new CountDownLatch(1);

        LogSink stuck = request -> {
            try {
                blocked.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        };

        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AsyncLogSink sink = new AsyncLogSink(stuck, asyncConfig(4), registry);

        for (int i = 0; i < 200; i++) {
            sink.send(request("log-" + i));
        }

        assertThat(sink.queued())
                .as("a full queue must stay bounded, not become an OOM")
                .isLessThanOrEqualTo(4);

        assertThat(registry.get("observability.logs.dropped").counter().count())
                .as("drops must be counted, so silent loss is still visible loss")
                .isPositive();

        blocked.countDown();
        sink.destroy();
    }

    @Test
    void aFailingDownstreamNeverReachesTheCaller() throws Exception {

        LogSink exploding = request -> {
            throw new IllegalStateException("logging-api is down");
        };

        AsyncLogSink sink = new AsyncLogSink(exploding, asyncConfig(10),
                new SimpleMeterRegistry());

        // The banking service keeps working regardless.
        sink.send(request("balance enquiry"));

        Thread.sleep(300);

        sink.destroy();
    }

    private static ObservabilityProperties.Async asyncConfig(int capacity) {

        ObservabilityProperties.Async config = new ObservabilityProperties.Async();
        config.setQueueCapacity(capacity);
        config.setWorkers(1);
        config.setShutdownTimeout(Duration.ofMillis(500));

        return config;
    }

    private static LogIngestRequest request(String message) {

        LogIngestRequest request = new LogIngestRequest();
        request.setMessage(message);
        request.setTraceId("TEST-TRACE");

        return request;
    }
}
