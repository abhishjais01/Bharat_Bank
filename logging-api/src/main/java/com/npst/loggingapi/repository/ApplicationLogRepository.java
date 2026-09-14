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

// database access for application_logs
@Repository
public interface ApplicationLogRepository
        extends JpaRepository<ApplicationLog, Long>, JpaSpecificationExecutor<ApplicationLog> {

    // all logs for a trace id
    List<ApplicationLog> findByTraceId(String traceId, Sort sort);

    // delete one batch of old rows, used by the retention job
    @Modifying
    @Query(value = "DELETE FROM application_logs WHERE created_at < :cutoff LIMIT :batchSize",
            nativeQuery = true)
    int deleteExpired(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);
}
