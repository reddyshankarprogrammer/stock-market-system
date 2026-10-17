-- =====================================================================
-- v2: one row per attempt to import a statement (first import and every
-- reprocess), with counts, warnings and the broker's own P&L summary for
-- reconciliation.
-- =====================================================================

CREATE TABLE statement_import_history (
    id                     VARCHAR(36)    NOT NULL,
    version                BIGINT         NOT NULL,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    created_by             VARCHAR(64),
    updated_by             VARCHAR(64),
    statement_id           VARCHAR(36)    NOT NULL,
    attempt_number         INT            NOT NULL,
    status                 VARCHAR(20)    NOT NULL,
    parser_name            VARCHAR(50)    NOT NULL,
    started_at             DATETIME(6)    NOT NULL,
    finished_at            DATETIME(6),
    rows_read              INT            NOT NULL DEFAULT 0,
    rows_skipped           INT            NOT NULL DEFAULT 0,
    trades_imported        INT            NOT NULL DEFAULT 0,
    duplicate_trades       INT            NOT NULL DEFAULT 0,
    charges_imported       INT            NOT NULL DEFAULT 0,
    taxes_imported         INT            NOT NULL DEFAULT 0,
    reported_realized_pnl  DECIMAL(19, 2),
    reported_unrealized_pnl DECIMAL(19, 2),
    reported_charges       DECIMAL(19, 2),
    reported_taxes         DECIMAL(19, 2),
    reported_net_pnl       DECIMAL(19, 2),
    warnings               MEDIUMTEXT,
    error_message          TEXT,
    CONSTRAINT pk_statement_import_history PRIMARY KEY (id),
    CONSTRAINT fk_import_history_statement FOREIGN KEY (statement_id) REFERENCES account_statements (id),
    CONSTRAINT uk_import_history_attempt UNIQUE (statement_id, attempt_number)
) ENGINE = InnoDB;
