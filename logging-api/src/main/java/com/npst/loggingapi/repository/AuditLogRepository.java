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

@Repository
public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByTraceId(String traceId, Sort sort);

    /**
     * Reads the tail of the hash chain under a write lock.
     *
     * <p>The lock serialises audit inserts, which is the price of a chain that
     * cannot fork: two concurrent writers reading the same previous hash would
     * produce two rows claiming the same parent, and the chain would no longer
     * prove anything. Audit volume is a fraction of application-log volume, so
     * correctness wins here.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AuditLog a WHERE a.id = (SELECT MAX(b.id) FROM AuditLog b)")
    Optional<AuditLog> findChainTipForUpdate();
}
