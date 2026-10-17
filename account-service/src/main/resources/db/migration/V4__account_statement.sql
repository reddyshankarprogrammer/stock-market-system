-- =====================================================================
-- v2: uploaded broker statements (metadata; the file itself lives under
-- account.statements.storage-dir, by default uploads/statements/).
-- =====================================================================

CREATE TABLE account_statements (
    id                 VARCHAR(36)  NOT NULL,
    version            BIGINT       NOT NULL,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    created_by         VARCHAR(64),
    updated_by         VARCHAR(64),
    account_id         VARCHAR(36)  NOT NULL,
    user_id            VARCHAR(64)  NOT NULL,
    broker_name        VARCHAR(30)  NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_path       VARCHAR(500) NOT NULL,
    content_type       VARCHAR(100),
    file_format        VARCHAR(10)  NOT NULL,
    file_size          BIGINT       NOT NULL,
    checksum_sha256    VARCHAR(64)  NOT NULL,
    status             VARCHAR(30)  NOT NULL,
    period_from        DATE,
    period_to          DATE,
    uploaded_at        DATETIME(6)  NOT NULL,
    processed_at       DATETIME(6),
    CONSTRAINT pk_account_statements PRIMARY KEY (id),
    CONSTRAINT fk_statements_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id),
    CONSTRAINT uk_statements_account_checksum UNIQUE (account_id, checksum_sha256),
    CONSTRAINT ck_statements_file_size CHECK (file_size > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_statements_account_uploaded ON account_statements (account_id, uploaded_at);
CREATE INDEX idx_statements_user ON account_statements (user_id);
