package com.npst.observability.sink;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

// counters for submitted, sent, failed and dropped records
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
