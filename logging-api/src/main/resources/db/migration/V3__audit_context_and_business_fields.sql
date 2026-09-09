-- ---------------------------------------------------------------------------
-- Layer 2: everything an auditor actually asks for.
--
-- V2 created audit_logs with the bare "who did what" columns. A real audit
-- question is narrower than that: "which device, on which channel, from which
-- IP, moved how much money for which customer, and what did the system answer".
-- This migration adds those.
--
-- Note the deliberate asymmetry with application_logs below. audit_logs gets
-- every context field as a first-class indexed column because compliance
-- queries them. application_logs gets only customer_id and channel; it is the
-- high-volume table and the rest belong in its metadata JSON, where they stay
-- readable without costing a column on millions of rows.
--
-- PII policy, decided explicitly: audit_logs stores customer_id and
-- mobile_number UNMASKED. It is a compliance record - a regulator asking who
-- moved money cannot work with XXXXXXXX5510. application_logs stays fully
-- masked. This is why the two tables have different grants.
-- ---------------------------------------------------------------------------

ALTER TABLE audit_logs

    -- Where the request came from.
    ADD COLUMN channel          VARCHAR(16)     NULL AFTER service,
    ADD COLUMN device_id        VARCHAR(128)    NULL AFTER channel,
    -- 45 chars holds an IPv6 address in full.
    ADD COLUMN ip_address       VARCHAR(45)     NULL AFTER device_id,

    -- Who it was for. Unmasked, by policy - see the header.
    ADD COLUMN customer_id      VARCHAR(64)     NULL AFTER ip_address,
    ADD COLUMN mobile_number    VARCHAR(20)     NULL AFTER customer_id,

    -- Which banking domain, from @LogRegistry(module = ...).
    ADD COLUMN module           VARCHAR(64)     NULL AFTER action,

    -- The API call itself.
    ADD COLUMN api_endpoint     VARCHAR(255)    NULL AFTER description,
    ADD COLUMN api_method       VARCHAR(8)      NULL AFTER api_endpoint,
    ADD COLUMN status_code      SMALLINT        NULL AFTER api_method,
    ADD COLUMN response_message VARCHAR(512)    NULL AFTER status_code,
    ADD COLUMN duration_ms      INT             NULL AFTER response_message,

    -- Business fields for a transaction. Promoted to columns rather than left
    -- in JSON because "all transfers above 5 lakh last quarter" is a question
    -- compliance actually asks.
    ADD COLUMN business_ref     VARCHAR(64)     NULL AFTER duration_ms,
    ADD COLUMN amount           DECIMAL(18,2)   NULL AFTER business_ref,
    ADD COLUMN currency         CHAR(3)         NULL AFTER amount,

    -- Domain-specific extras that do not deserve a column of their own.
    ADD COLUMN business_context JSON            NULL AFTER currency;


-- Indexes for the questions compliance asks, each paired with time because an
-- audit query is always bounded by a period.
CREATE INDEX idx_audit_logs_customer_time ON audit_logs (customer_id, created_at);
CREATE INDEX idx_audit_logs_channel_time  ON audit_logs (channel, created_at);
CREATE INDEX idx_audit_logs_module_action ON audit_logs (module, action);
CREATE INDEX idx_audit_logs_business_ref  ON audit_logs (business_ref);


-- ---------------------------------------------------------------------------
-- application_logs: only the two fields support genuinely filters on.
-- "Show me everything for this customer" and "show me the mobile channel" are
-- real support queries; device id and IP are diagnostic detail and live in
-- metadata.
-- ---------------------------------------------------------------------------

ALTER TABLE application_logs
    ADD COLUMN customer_id VARCHAR(64) NULL AFTER service,
    ADD COLUMN channel     VARCHAR(16) NULL AFTER customer_id;

CREATE INDEX idx_app_logs_customer_time ON application_logs (customer_id, created_at);
CREATE INDEX idx_app_logs_channel_time  ON application_logs (channel, created_at);
