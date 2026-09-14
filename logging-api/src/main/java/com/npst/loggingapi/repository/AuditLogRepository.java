package com.npst.loggingapi.repository;

import com.npst.loggingapi.entity.AuditLog;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// database access for audit_logs
@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    // all audit records for a trace id
    List<AuditLog> findByTraceId(String traceId, Sort sort);

    // latest row, locked so two inserts can't use the same previous hash
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuditLog a WHERE a.id = (SELECT MAX(b.id) FROM AuditLog b)")
    Optional<AuditLog> findChainTipForUpdate();
}
