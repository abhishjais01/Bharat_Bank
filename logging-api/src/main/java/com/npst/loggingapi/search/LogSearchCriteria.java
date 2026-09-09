package com.npst.loggingapi.search;

import java.time.Instant;

/**
 * Filters for an application-log search. Every field is optional; supplying
 * none returns the most recent logs.
 */
public record LogSearchCriteria(
        String service,
        String level,
        String environment,
        String customerId,
        String channel,
        Instant from,
        Instant to) {
}
