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

// nightly job that deletes old application logs (never audit logs), off by default
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

    // runs on the configured schedule
    @Scheduled(cron = "${observability.retention.cron:0 30 2 * * *}")
    @Transactional
    public void purgeExpiredApplicationLogs() {

        // anything older than this is deleted
        Instant cutoff = Instant.now().minus(retention);

        // delete in batches so ingest isn't blocked by one huge delete
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
