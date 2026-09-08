-- ---------------------------------------------------------------------------
-- Layer 2 hook: the audit trail table.
--
-- Created now, written to later. Nothing in Layer 1 inserts here - the SDK's
-- audit() path is deliberately not shipped to logging-api yet. The table
-- exists at this point so that turning audit on becomes a feature flag rather
-- than a schema migration against a live log store.
--
-- Three decisions are baked in and are hard to retrofit:
--
--  1. A SEPARATE TABLE. Audit is not "application logs with a different
--     event_type". It answers "who did what", is read by compliance rather
--     than support, and has a retention obligation measured in years rather
--     than days. Mixing the two makes both harder to secure and to purge.
--
--  2. APPEND-ONLY, ENFORCED BY THE DATABASE. The triggers below reject any
--     UPDATE or DELETE outright. Immutability by convention is not
--     immutability; a trigger survives an application bug, a careless
--     migration, and an operator with a MySQL prompt.
--
--  3. HASH CHAINED. Each row carries the hash of the previous row, so
--     removing or altering history is detectable even by someone who can drop
--     the triggers. This is what makes the trail tamper-evident rather than
--     merely tamper-resistant.
--
-- The before/after columns come straight from the PRD's Auditability NFR:
-- "all admin actions and financial transactions are logged with actor,
-- timestamp, and before/after state where applicable".
-- ---------------------------------------------------------------------------

CREATE TABLE audit_logs (

    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    trace_id        VARCHAR(64)     NOT NULL,

    bank_code       VARCHAR(16)     NOT NULL,
    environment     VARCHAR(16)     NOT NULL,
    service         VARCHAR(64)     NOT NULL,

    -- Who acted.
    actor_id        VARCHAR(64)     NOT NULL,
    actor_type      VARCHAR(32)     NOT NULL,

    -- What they did, and to which record.
    action          VARCHAR(64)     NOT NULL,
    entity          VARCHAR(64)     NOT NULL,
    entity_id       VARCHAR(64)     NULL,
    description     TEXT            NULL,

    -- State transition, where the action has one.
    before_state    JSON            NULL,
    after_state     JSON            NULL,

    event_time      DATETIME(3)     NOT NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    -- Tamper evidence. prev_hash is NULL only for the first row in the chain.
    prev_hash       CHAR(64)        NULL,
    row_hash        CHAR(64)        NOT NULL,

    PRIMARY KEY (id),

    KEY idx_audit_logs_trace (trace_id),
    KEY idx_audit_logs_actor_time (actor_id, created_at),
    KEY idx_audit_logs_entity (entity, entity_id),
    KEY idx_audit_logs_created (created_at)

) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- An audit row, once written, is final.

CREATE TRIGGER audit_logs_block_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_logs is append-only: UPDATE is not permitted';


CREATE TRIGGER audit_logs_block_delete
    BEFORE DELETE ON audit_logs
    FOR EACH ROW
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_logs is append-only: DELETE is not permitted';
