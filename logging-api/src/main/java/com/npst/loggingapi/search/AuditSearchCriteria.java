package com.npst.loggingapi.search;

import java.time.Instant;

// filters accepted by the audit search
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
