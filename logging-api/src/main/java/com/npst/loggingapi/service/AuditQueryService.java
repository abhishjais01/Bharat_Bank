package com.npst.loggingapi.service;

import com.npst.loggingapi.entity.AuditLog;
import com.npst.loggingapi.repository.AuditLogRepository;
import com.npst.loggingapi.search.AuditSearchCriteria;
import com.npst.loggingapi.search.LogSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// read-only queries on audit_logs
@Service
public class AuditQueryService {

    private final AuditLogRepository repository;

    public AuditQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    // records for one trace, oldest first
    @Transactional(readOnly = true)
    public List<AuditLog> findByTraceId(String traceId) {
        return repository.findByTraceId(traceId, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
    }

    // filtered, paged search
    @Transactional(readOnly = true)
    public Page<AuditLog> search(AuditSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(LogSpecifications.matching(criteria), pageable);
    }
}
