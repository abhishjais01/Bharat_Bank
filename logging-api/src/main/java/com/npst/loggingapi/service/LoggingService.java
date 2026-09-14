package com.npst.loggingapi.service;

import com.npst.loggingapi.entity.ApplicationLog;
import com.npst.loggingapi.exception.UnsupportedEventTypeException;
import com.npst.loggingapi.mapper.LogMapper;
import com.npst.loggingapi.repository.ApplicationLogRepository;
import com.npst.observability.contract.EventType;
import com.npst.observability.contract.LogIngestRequest;
import com.npst.loggingapi.search.LogSearchCriteria;
import com.npst.loggingapi.search.LogSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// stores and searches application logs
@Service
public class LoggingService {

    private final ApplicationLogRepository repository;
    private final LogMapper mapper;

    public LoggingService(ApplicationLogRepository repository, LogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    // save one application log
    @Transactional
    public Long store(LogIngestRequest request) {

        // only application logs go to this table, audit has its own endpoint
        if (request.getEventType() != EventType.APPLICATION) {
            throw new UnsupportedEventTypeException(request.getEventType());
        }

        // request -> row -> insert
        ApplicationLog saved = repository.save(mapper.toEntity(request));

        return saved.getId();
    }

    // logs for one trace, oldest first
    @Transactional(readOnly = true)
    public List<ApplicationLog> findByTraceId(String traceId) {
        return repository.findByTraceId(traceId, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
    }

    // filtered, paged search
    @Transactional(readOnly = true)
    public Page<ApplicationLog> search(LogSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(LogSpecifications.matching(criteria), pageable);
    }
}
