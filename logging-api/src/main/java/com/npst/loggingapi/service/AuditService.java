package com.npst.loggingapi.service;

import com.npst.loggingapi.entity.AuditLog;
import com.npst.loggingapi.mapper.AuditLogMapper;
import com.npst.loggingapi.repository.AuditLogRepository;
import com.npst.observability.contract.AuditIngestRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// saves audit records and links them into the hash chain
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

    // one transaction: read the last hash, build the row, hash it, insert it
    @Transactional
    public Long append(AuditIngestRequest request) {

        // chain tip is read under a write lock so two inserts can't get the same parent hash
        String previousHash = repository.findChainTipForUpdate()
                .map(AuditLog::getRowHash)
                .orElse(null);

        // request -> row (private details masked)
        AuditLog entry = mapper.toEntity(request);

        // link to the previous row and seal this one
        entry.setPrevHash(previousHash);
        entry.setRowHash(hasher.hash(previousHash, entry));

        // insert; the lock is released on commit
        return repository.save(entry).getId();
    }
}
