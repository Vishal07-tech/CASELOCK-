-- ============================================================================
-- CaseLock - Reference MySQL Schema
-- ============================================================================
-- This file is DOCUMENTATION, not an executable migration. The application
-- creates and evolves this exact schema automatically via Hibernate
-- (spring.jpa.hibernate.ddl-auto=update in application.yml) by reading the
-- JPA entity classes under com.caselock.entity. It is included so you can:
--   1. Review the schema without reading every entity class.
--   2. Hand this to a DBA, or adapt it into a Flyway/Liquibase migration if
--      you later want managed migrations instead of Hibernate auto-DDL
--      (recommended for a real production deployment - ddl-auto=validate
--      is what the "prod" Spring profile uses for that reason).
--
-- Run manually only if you want to pre-create the schema by hand; otherwise
-- just create an empty `caselock_db` database and let Hibernate do the rest.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS caselock_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE caselock_db;

-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    username                VARCHAR(50)  NOT NULL UNIQUE,
    email                   VARCHAR(120) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    full_name               VARCHAR(120) NOT NULL,
    badge_number            VARCHAR(50),
    department              VARCHAR(120),
    phone_number            VARCHAR(30),
    role                    VARCHAR(30)  NOT NULL,   -- ADMIN | INVESTIGATOR | FORENSIC_ANALYST | LEGAL_OFFICER | VIEWER
    account_status          VARCHAR(20)  NOT NULL,   -- ACTIVE | DISABLED | LOCKED
    last_login_at           DATETIME,
    last_login_ip           VARCHAR(64),
    failed_login_attempts   INT NOT NULL DEFAULT 0,
    locked_until            DATETIME,
    created_at              DATETIME NOT NULL,
    updated_at              DATETIME NOT NULL,
    INDEX idx_user_username (username),
    INDEX idx_user_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
CREATE TABLE cases (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_number         VARCHAR(40)  NOT NULL UNIQUE,
    title               VARCHAR(200) NOT NULL,
    description         TEXT,
    case_type           VARCHAR(40)  NOT NULL,
    priority            VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status              VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    investigator_id     BIGINT NOT NULL,
    location            VARCHAR(200),
    case_start_date     DATE,
    case_closing_date   DATE,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NOT NULL,
    INDEX idx_case_number (case_number),
    INDEX idx_case_status (status),
    INDEX idx_case_created_at (created_at),
    CONSTRAINT fk_case_investigator FOREIGN KEY (investigator_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE case_team_members (
    case_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (case_id, user_id),
    CONSTRAINT fk_ctm_case FOREIGN KEY (case_id) REFERENCES cases (id),
    CONSTRAINT fk_ctm_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
CREATE TABLE evidence (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    evidence_number         VARCHAR(40)  NOT NULL UNIQUE,
    case_id                 BIGINT NOT NULL,
    file_name               VARCHAR(255) NOT NULL,
    stored_file_name        VARCHAR(255) NOT NULL,
    file_type               VARCHAR(100),
    file_size_bytes         BIGINT NOT NULL,
    uploaded_by             BIGINT NOT NULL,
    upload_date             DATETIME NOT NULL,
    description             TEXT,
    evidence_category       VARCHAR(30) NOT NULL,
    current_custodian_id    BIGINT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    original_sha256         CHAR(64) NOT NULL,   -- immutable once written
    current_sha256          CHAR(64),
    integrity_status        VARCHAR(20) NOT NULL DEFAULT 'NOT_YET_VERIFIED',
    last_verified_at        DATETIME,
    is_sealed               BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              DATETIME NOT NULL,
    updated_at              DATETIME NOT NULL,
    INDEX idx_evidence_number (evidence_number),
    INDEX idx_evidence_case (case_id),
    INDEX idx_evidence_hash (original_sha256),
    INDEX idx_evidence_file_name (file_name),
    INDEX idx_evidence_status (status),
    CONSTRAINT fk_evidence_case FOREIGN KEY (case_id) REFERENCES cases (id),
    CONSTRAINT fk_evidence_uploader FOREIGN KEY (uploaded_by) REFERENCES users (id),
    CONSTRAINT fk_evidence_custodian FOREIGN KEY (current_custodian_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
CREATE TABLE chain_of_custody_events (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    evidence_id         BIGINT NOT NULL,
    from_user_id        BIGINT,
    to_user_id          BIGINT,
    action              VARCHAR(20) NOT NULL,  -- COLLECTED|UPLOADED|ACCESSED|TRANSFERRED|ANALYZED|VERIFIED|DOWNLOADED|RETURNED|SEALED|REOPENED|ARCHIVED
    event_timestamp     DATETIME NOT NULL,
    reason              VARCHAR(500),
    location            VARCHAR(200),
    remarks             TEXT,
    hash_at_transfer    CHAR(64),
    ip_address          VARCHAR(64),
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NOT NULL,
    INDEX idx_custody_evidence (evidence_id),
    INDEX idx_custody_timestamp (event_timestamp),
    CONSTRAINT fk_custody_evidence FOREIGN KEY (evidence_id) REFERENCES evidence (id),
    CONSTRAINT fk_custody_from_user FOREIGN KEY (from_user_id) REFERENCES users (id),
    CONSTRAINT fk_custody_to_user FOREIGN KEY (to_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT,
    action              VARCHAR(40) NOT NULL,
    entity_type         VARCHAR(40),
    entity_id           BIGINT,
    description         TEXT,
    ip_address          VARCHAR(64),
    result              VARCHAR(20) NOT NULL,   -- SUCCESS | FAILURE | DENIED
    event_timestamp     DATETIME NOT NULL,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NOT NULL,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_timestamp (created_at),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
CREATE TABLE notifications (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id            BIGINT NOT NULL,
    type                    VARCHAR(40) NOT NULL,
    title                   VARCHAR(200) NOT NULL,
    message                 VARCHAR(500) NOT NULL,
    related_case_id         BIGINT,
    related_evidence_id     BIGINT,
    is_read                 BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              DATETIME NOT NULL,
    updated_at              DATETIME NOT NULL,
    INDEX idx_notification_recipient (recipient_id),
    INDEX idx_notification_read (is_read),
    CONSTRAINT fk_notification_recipient FOREIGN KEY (recipient_id) REFERENCES users (id)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
CREATE TABLE login_history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT,
    username            VARCHAR(50),
    successful          BOOLEAN NOT NULL,
    ip_address          VARCHAR(64),
    user_agent          VARCHAR(255),
    failure_reason      VARCHAR(200),
    attempted_at        DATETIME NOT NULL,
    created_at          DATETIME NOT NULL,
    updated_at          DATETIME NOT NULL,
    INDEX idx_login_history_user (user_id),
    INDEX idx_login_history_time (attempted_at),
    CONSTRAINT fk_login_history_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;
