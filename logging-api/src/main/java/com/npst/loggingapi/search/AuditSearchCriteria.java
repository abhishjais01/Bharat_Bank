package com.npst.loggingapi.search;

import java.time.Instant;

/**
 * Filters for an audit search - shaped around the questions compliance asks
 * rather than the columns that happen to exist.
 */
public record AuditSearchCriteria(
        String actorId,
        String customerId,
        String action,
        String module,
        String entity,
        String entityId,
        String channel,
        String businessRef,
        Instant from,
        Instant to) {
}
