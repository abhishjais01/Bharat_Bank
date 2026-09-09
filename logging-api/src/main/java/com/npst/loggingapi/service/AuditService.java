package com.npst.loggingapi.service;

import com.npst.loggingapi.entity.AuditLog;
import com.npst.loggingapi.mapper.AuditLogMapper;
import com.npst.loggingapi.repository.AuditLogRepository;
import com.npst.observability.contract.AuditIngestRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;
    private final AuditHasher hasher;

    public AuditService(AuditLogRepository repository,
                        AuditLogMapper mapper,
                        AuditHasher hasher) {
        this.repository = repository;
        this.mapper = mapper;
        this.hasher = hasher;
    }

    /**
     * Appends one audit record to the chain.
     *
     * <p>Transactional because reading the chain tip and writing the next link
     * have to be one atomic step; the repository takes a write lock on the tip
     * so two concurrent writers cannot both claim the same parent.
     */
    @Transactional
    public Long append(AuditIngestRequest request) {

        String previousHash = repository.findChainTipForUpdate()
                .map(AuditLog::getRowHash)
                .orElse(null);

        AuditLog entry = mapper.toEntity(request);

        entry.setPrevHash(previousHash);
        entry.setRowHash(hasher.hash(previousHash, entry));

        return repository.save(entry).getId();
    }
}
