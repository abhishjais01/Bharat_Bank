package com.npst.loggingapi.service;

import com.npst.loggingapi.entity.ApplicationLog;
import com.npst.loggingapi.exception.UnsupportedEventTypeException;
import com.npst.loggingapi.mapper.LogMapper;
import com.npst.loggingapi.repository.ApplicationLogRepository;
import com.npst.observability.contract.EventType;
import com.npst.observability.contract.LogIngestRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
