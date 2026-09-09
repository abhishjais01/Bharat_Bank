package com.npst.loggingapi.repository;

import com.npst.loggingapi.entity.ApplicationLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ApplicationLogRepository
        extends JpaRepository<ApplicationLog, Long>, JpaSpecificationExecutor<ApplicationLog> {

    /**
     * Every line of one customer journey.
     *
     * <p>Returns a list, not a single row: a trace spans several services and
     * many lines, and the whole point of the trace id is to get all of them
     * back together.
     */
    List<ApplicationLog> findByTraceId(String traceId, Sort sort);

    /**
     * Deletes one batch of expired rows.
     *
     * <p>Native and batched deliberately. A single DELETE across millions of
     * rows holds locks long enough to stall ingest, and this service must never
     * be the reason something else is slow.
     */
    @Modifying
    @Query(value = "DELETE FROM application_logs WHERE created_at < :cutoff LIMIT :batchSize",
            nativeQuery = true)
    int deleteExpired(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
}
