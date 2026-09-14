package com.npst.loggingapi.search;

import java.time.Instant;

// filters accepted by the log search
public record LogSearchCriteria(
        String service,
        String level,
        String environment,
        String customerId,
        String channel,
        Instant from,
        Instant to) {
}
