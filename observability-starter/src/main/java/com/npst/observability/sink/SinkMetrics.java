package com.npst.observability.sink;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Counters for the log pipeline itself.
 *
 * <p>Without these an outage is invisible: logs simply stop appearing, and
 * nobody can tell whether the service went quiet or the pipeline broke.
 * {@code observability_logs_dropped_total} climbing is the signal that the
 * queue is saturated and lines are being discarded on purpose.
 *
 * <p>A MeterRegistry is optional - a service without Micrometer still logs, it
 * just publishes no counters.
 */
final class SinkMetrics {

    private static final String PREFIX = "observability.logs.";

    private SinkMetrics() {
    }

    static Counter counter(MeterRegistry registry, String name, String description) {

        if (registry == null) {
            return null;
        }

        return Counter.builder(PREFIX + name)
                .description(description)
                .register(registry);
    }

    static void increment(Counter counter) {
        if (counter != null) {
            counter.increment();
        }
    }
}
