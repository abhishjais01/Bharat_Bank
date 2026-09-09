-- ---------------------------------------------------------------------------
-- Bootstrap for the observability database.
--
-- Creates the database and the application user that logging-api connects as.
-- It does NOT create any tables - Flyway owns the schema and runs its
-- migrations on startup.
--
-- Run this once against a fresh MySQL 8 instance:
--
--     mysql -u root -p < infrastructure/mysql-init.sql
--
-- The Docker Compose MySQL creates the database and user from its own
-- environment variables, so this script is only needed when pointing at a
-- MySQL that was installed directly on the host.
-- ---------------------------------------------------------------------------

-- MySQL 8 refuses CREATE TRIGGER from a non-SUPER user while binary logging is
-- on, which it is by default. V2__audit_logs.sql creates the append-only audit
-- triggers, so without this the migration fails with:
--   "You do not have the SUPER privilege and binary logging is enabled"
-- SET PERSIST survives a restart, unlike SET GLOBAL.
SET PERSIST log_bin_trust_function_creators = 1;

CREATE DATABASE IF NOT EXISTS observability
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Matches the defaults in logging-api/application.yml, both overridable by
-- MYSQL_USER / MYSQL_PASSWORD.
CREATE USER IF NOT EXISTS 'npst'@'localhost' IDENTIFIED BY 'npst123';
CREATE USER IF NOT EXISTS 'npst'@'%'         IDENTIFIED BY 'npst123';

-- Flyway needs DDL rights to create tables, indexes and the audit triggers.
GRANT ALL PRIVILEGES ON observability.* TO 'npst'@'localhost';
GRANT ALL PRIVILEGES ON observability.* TO 'npst'@'%';

-- Creating a trigger requires this explicitly; without it V2__audit_logs.sql
-- fails with "Access denied; you need the SUPER or TRIGGER privilege".
GRANT TRIGGER ON observability.* TO 'npst'@'localhost';
GRANT TRIGGER ON observability.* TO 'npst'@'%';

FLUSH PRIVILEGES;

SELECT 'observability database and npst user are ready' AS status;
