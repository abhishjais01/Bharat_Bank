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

@Service
public class LoggingService {

    private final ApplicationLogRepository repository;
    private final LogMapper mapper;

    public LoggingService(ApplicationLogRepository repository, LogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional
    public Long store(LogIngestRequest request) {

        // The Layer 1 boundary, enforced at the service rather than left to a
        // convention the SDK happens to follow today.
        if (request.getEventType() != EventType.APPLICATION) {
            throw new UnsupportedEventTypeException(request.getEventType());
        }

        ApplicationLog saved = repository.save(mapper.toEntity(request));

        return saved.getId();
    }

    /**
     * Every line of one customer journey, oldest first.
     *
     * <p>Ordered by ingest time rather than event time: clocks across services
     * drift, and a trace read in the wrong order is worse than useless during
     * an incident.
     */
    @Transactional(readOnly = true)
    public List<ApplicationLog> findByTraceId(String traceId) {
        return repository.findByTraceId(traceId, Sort.by(Sort.Direction.ASC, "createdAt", "id"));
    }

    @Transactional(readOnly = true)
    public Page<ApplicationLog> search(LogSearchCriteria criteria, Pageable pageable) {
        return repository.findAll(LogSpecifications.matching(criteria), pageable);
    }
}
