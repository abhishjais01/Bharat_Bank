-- needed for the audit triggers created by V2
SET PERSIST log_bin_trust_function_creators = 1;

CREATE DATABASE IF NOT EXISTS observability
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'npst'@'localhost' IDENTIFIED BY 'npst123';
CREATE USER IF NOT EXISTS 'npst'@'%'         IDENTIFIED BY 'npst123';

GRANT ALL PRIVILEGES ON observability.* TO 'npst'@'localhost';
GRANT ALL PRIVILEGES ON observability.* TO 'npst'@'%';

GRANT TRIGGER ON observability.* TO 'npst'@'localhost';
GRANT TRIGGER ON observability.* TO 'npst'@'%';

FLUSH PRIVILEGES;

SELECT 'observability database and npst user are ready' AS status;
