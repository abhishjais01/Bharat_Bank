package com.npst.loggingapi.service;

import com.npst.loggingapi.repository.ApplicationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Deletes application logs past their retention window.
 *
 * <p><b>It touches application_logs and nothing else.</b> That is the entire
 * reason this class names its table explicitly instead of taking it as a
 * parameter: audit_logs carries a retention obligation measured in years, and a
 * purge job that could be pointed at the wrong table is a purge job that
 * eventually will be. The database would refuse the DELETE anyway - the
 * append-only triggers see to that - but relying on the last line of defence
 * for a first-line mistake is not a design.
 *
 * <p>Off by default. A retention policy is a decision the bank makes, not a
 * default a library should apply to somebody's data.
 */
@Component
@ConditionalOnProperty(prefix = "observability.retention", name = "enabled",
        havingValue = "true")
public class LogRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(LogRetentionJob.class);

    private final ApplicationLogRepository repository;
    private final Duration retention;
    private final int batchSize;

    public LogRetentionJob(ApplicationLogRepository repository,
                           @Value("${observability.retention.period:P90D}") Duration retention,
                           @Value("${observability.retention.batch-size:5000}") int batchSize) {
        this.repository = repository;
        this.retention = retention;
        this.batchSize = batchSize;
    }

    /**
     * Runs nightly, off-peak.
     *
     * <p>Deletes in batches rather than one enormous statement: a single DELETE
     * spanning millions of rows holds locks long enough to stall ingest, and
     * the whole point of this service is that it never becomes the reason
     * something else is slow.
     */
    @Scheduled(cron = "${observability.retention.cron:0 30 2 * * *}")
    @Transactional
    public void purgeExpiredApplicationLogs() {

        Instant cutoff = Instant.now().minus(retention);

        int deleted = repository.deleteExpired(cutoff, batchSize);
        int total = deleted;

        while (deleted == batchSize) {
            deleted = repository.deleteExpired(cutoff, batchSize);
            total += deleted;
        }

        if (total > 0) {
            log.info("Retention purge removed {} application log(s) older than {} (cutoff {})",
                    total, retention, cutoff);
        }
    }
}
