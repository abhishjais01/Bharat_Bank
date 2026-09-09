package com.npst.loggingapi.repository;

import com.npst.loggingapi.entity.ApplicationLog;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}
