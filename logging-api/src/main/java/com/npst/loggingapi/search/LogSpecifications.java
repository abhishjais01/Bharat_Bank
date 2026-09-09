package com.npst.loggingapi.search;

import com.npst.loggingapi.entity.ApplicationLog;
import com.npst.loggingapi.entity.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause from whichever filters were supplied.
 *
 * <p>Specifications rather than a pile of derived query methods: the filters
 * combine freely, and {@code findByServiceAndLevelAndCustomerIdAndChannelAnd...}
 * does not scale past about three.
 */
public final class LogSpecifications {

    private LogSpecifications() {
    }

    public static Specification<ApplicationLog> matching(LogSearchCriteria criteria) {

        return (root, query, builder) -> {

            List<Predicate> predicates = new ArrayList<>();

            equalIfPresent(predicates, builder, root.get("service"), criteria.service());
            equalIfPresent(predicates, builder, root.get("level"), upper(criteria.level()));
            equalIfPresent(predicates, builder, root.get("environment"), upper(criteria.environment()));
            equalIfPresent(predicates, builder, root.get("customerId"), criteria.customerId());
            equalIfPresent(predicates, builder, root.get("channel"), upper(criteria.channel()));

            addWindow(predicates, builder, root.get("createdAt"), criteria.from(), criteria.to());

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<AuditLog> matching(AuditSearchCriteria criteria) {

        return (root, query, builder) -> {

            List<Predicate> predicates = new ArrayList<>();

            equalIfPresent(predicates, builder, root.get("actorId"), criteria.actorId());
            equalIfPresent(predicates, builder, root.get("customerId"), criteria.customerId());
            equalIfPresent(predicates, builder, root.get("action"), upper(criteria.action()));
            equalIfPresent(predicates, builder, root.get("module"), upper(criteria.module()));
            equalIfPresent(predicates, builder, root.get("entity"), criteria.entity());
            equalIfPresent(predicates, builder, root.get("entityId"), criteria.entityId());
            equalIfPresent(predicates, builder, root.get("channel"), upper(criteria.channel()));
            equalIfPresent(predicates, builder, root.get("businessRef"), criteria.businessRef());

            addWindow(predicates, builder, root.get("createdAt"), criteria.from(), criteria.to());

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void equalIfPresent(List<Predicate> predicates,
                                       jakarta.persistence.criteria.CriteriaBuilder builder,
                                       jakarta.persistence.criteria.Path<?> path,
                                       String value) {

        if (value != null && !value.isBlank()) {
            predicates.add(builder.equal(path, value));
        }
    }

    private static void addWindow(List<Predicate> predicates,
                                  jakarta.persistence.criteria.CriteriaBuilder builder,
                                  jakarta.persistence.criteria.Path<Instant> path,
                                  Instant from,
                                  Instant to) {

        if (from != null) {
            predicates.add(builder.greaterThanOrEqualTo(path, from));
        }

        if (to != null) {
            predicates.add(builder.lessThanOrEqualTo(path, to));
        }
    }

    /** Levels, channels, actions and modules are stored upper case. */
    private static String upper(String value) {
        return value == null ? null : value.toUpperCase();
    }
}
