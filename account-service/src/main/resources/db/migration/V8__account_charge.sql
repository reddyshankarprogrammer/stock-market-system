-- =====================================================================
-- v2: charges recorded from broker statements and manual trades.
--
-- Fees charged by this platform's own fee engine stay in account_fees;
-- the charge summary API reports both.
-- =====================================================================

CREATE TABLE account_charges (
    id            VARCHAR(36)    NOT NULL,
    version       BIGINT         NOT NULL,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    created_by    VARCHAR(64),
    updated_by    VARCHAR(64),
    account_id    VARCHAR(36)    NOT NULL,
    user_id       VARCHAR(64)    NOT NULL,
    charge_type   VARCHAR(20)    NOT NULL,
    amount        DECIMAL(19, 2) NOT NULL,
    charge_date   DATE           NOT NULL,
    source        VARCHAR(20)    NOT NULL,
    trade_id      VARCHAR(36),
    statement_id  VARCHAR(36),
    description   VARCHAR(255),
    CONSTRAINT pk_account_charges PRIMARY KEY (id),
    CONSTRAINT fk_charges_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id),
    CONSTRAINT fk_charges_trade FOREIGN KEY (trade_id) REFERENCES account_trades (id),
    CONSTRAINT fk_charges_statement FOREIGN KEY (statement_id) REFERENCES account_statements (id),
    CONSTRAINT ck_charges_amount CHECK (amount >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_charges_account_date ON account_charges (account_id, charge_date);
CREATE INDEX idx_charges_type ON account_charges (charge_type);
CREATE INDEX idx_charges_statement ON account_charges (statement_id);
