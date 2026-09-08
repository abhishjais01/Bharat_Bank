-- ---------------------------------------------------------------------------
-- Layer 1: central storage for application logs.
--
-- This table is what lets production support answer "what happened during this
-- customer's balance enquiry" from a trace id alone, after the Loki retention
-- window has passed.
--
-- Flyway owns this schema. It replaces ddl-auto=update, which meant the table
-- existed only because Hibernate happened to create it on someone's laptop -
-- with no review, no history, and no guarantee two environments matched.
-- ---------------------------------------------------------------------------

CREATE TABLE application_logs (

    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- Correlation id, minted at the gateway and carried through every service.
    trace_id        VARCHAR(64)     NOT NULL,

    -- Platform identity, stamped by the SDK. Never supplied by a controller.
    bank_code       VARCHAR(16)     NOT NULL,
    environment     VARCHAR(16)     NOT NULL,
    service         VARCHAR(64)     NOT NULL,

    event_type      VARCHAR(16)     NOT NULL,
    `level`         VARCHAR(8)      NOT NULL,

    message         TEXT            NOT NULL,

    -- Business context, already masked by the SDK before it left the service.
    metadata        JSON            NULL,

    -- Wire format the producer used, so a rolling deployment can mix versions.
    schema_version  VARCHAR(8)      NOT NULL DEFAULT '1.0',

    -- When it happened in the emitting service.
    event_time      DATETIME(3)     NOT NULL,

    -- When it arrived here. Kept separate because clocks across microservices
    -- are never perfectly aligned, and ordering by ingest time is what makes a
    -- trace readable when one service's clock drifts.
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    -- The support path: paste a trace id, get the journey. Without this index
    -- every lookup is a full scan of a table growing at request rate.
    KEY idx_app_logs_trace (trace_id),

    -- The three filters the search API exposes, each paired with time because
    -- a log query is always bounded by a window in practice.
    KEY idx_app_logs_service_time (service, created_at),
    KEY idx_app_logs_level_time (`level`, created_at),
    KEY idx_app_logs_bank_time (bank_code, created_at),

    -- Supports the retention purge, which deletes by age alone.
    KEY idx_app_logs_created (created_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
