-- =====================================================================
-- account-service schema (MySQL 8)
--
-- Every table carries the BaseEntity columns:
--   id (UUID string), version (optimistic locking),
--   created_at / updated_at, created_by / updated_by (Spring Data auditing).
-- Table names are prefixed (account_*) because the schema is shared with the
-- other services (portfolio-service already owns a "holdings" table).
-- Hibernate runs with ddl-auto=validate, so this file is the source of truth.
-- =====================================================================

CREATE TABLE investment_accounts (
    id              VARCHAR(36)    NOT NULL,
    version         BIGINT         NOT NULL,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    account_number  VARCHAR(20)    NOT NULL,
    user_id         VARCHAR(64)    NOT NULL,
    account_name    VARCHAR(100)   NOT NULL,
    account_type    VARCHAR(20)    NOT NULL,
    currency        VARCHAR(3)     NOT NULL,
    balance         DECIMAL(19, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    closed_at       DATETIME(6),
    CONSTRAINT pk_investment_accounts PRIMARY KEY (id),
    CONSTRAINT uk_investment_accounts_number UNIQUE (account_number),
    CONSTRAINT ck_investment_accounts_balance CHECK (balance >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_investment_accounts_user_id ON investment_accounts (user_id);
CREATE INDEX idx_investment_accounts_status ON investment_accounts (status);

CREATE TABLE account_fund_transactions (
    id                VARCHAR(36)    NOT NULL,
    version           BIGINT         NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        VARCHAR(64),
    updated_by        VARCHAR(64),
    account_id        VARCHAR(36)    NOT NULL,
    transaction_type  VARCHAR(20)    NOT NULL,
    amount            DECIMAL(19, 2) NOT NULL,
    fee_amount        DECIMAL(19, 2) NOT NULL,
    balance_after     DECIMAL(19, 2) NOT NULL,
    reference         VARCHAR(100),
    status            VARCHAR(20)    NOT NULL,
    transaction_date  DATETIME(6)    NOT NULL,
    CONSTRAINT pk_account_fund_transactions PRIMARY KEY (id),
    CONSTRAINT fk_fund_tx_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id)
) ENGINE = InnoDB;

CREATE INDEX idx_fund_tx_account_date ON account_fund_transactions (account_id, transaction_date);
CREATE INDEX idx_fund_tx_type ON account_fund_transactions (transaction_type);

CREATE TABLE account_ledger (
    id                VARCHAR(36)    NOT NULL,
    version           BIGINT         NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        VARCHAR(64),
    updated_by        VARCHAR(64),
    account_id        VARCHAR(36)    NOT NULL,
    entry_number      BIGINT         NOT NULL,
    entry_type        VARCHAR(10)    NOT NULL,
    category          VARCHAR(20)    NOT NULL,
    amount            DECIMAL(19, 2) NOT NULL,
    balance_after     DECIMAL(19, 2) NOT NULL,
    reference_type    VARCHAR(20)    NOT NULL,
    reference_id      VARCHAR(36)    NOT NULL,
    description       VARCHAR(255),
    transaction_date  DATETIME(6)    NOT NULL,
    CONSTRAINT pk_account_ledger PRIMARY KEY (id),
    CONSTRAINT uk_ledger_account_entry UNIQUE (account_id, entry_number),
    CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id),
    CONSTRAINT ck_ledger_amount CHECK (amount > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_ledger_account_date ON account_ledger (account_id, transaction_date);
CREATE INDEX idx_ledger_reference ON account_ledger (reference_type, reference_id);

CREATE TABLE account_trades (
    id                     VARCHAR(36)    NOT NULL,
    version                BIGINT         NOT NULL,
    created_at             DATETIME(6)    NOT NULL,
    updated_at             DATETIME(6)    NOT NULL,
    created_by             VARCHAR(64),
    updated_by             VARCHAR(64),
    account_id             VARCHAR(36)    NOT NULL,
    symbol                 VARCHAR(20)    NOT NULL,
    side                   VARCHAR(10)    NOT NULL,
    quantity               INT            NOT NULL,
    price                  DECIMAL(19, 4) NOT NULL,
    gross_amount           DECIMAL(19, 2) NOT NULL,
    fee_amount             DECIMAL(19, 2) NOT NULL,
    tax_amount             DECIMAL(19, 2) NOT NULL,
    net_amount             DECIMAL(19, 2) NOT NULL,
    realized_profit        DECIMAL(19, 2),
    average_cost_at_trade  DECIMAL(19, 4),
    trade_date             DATETIME(6)    NOT NULL,
    CONSTRAINT pk_account_trades PRIMARY KEY (id),
    CONSTRAINT fk_trades_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id),
    CONSTRAINT ck_trades_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

CREATE INDEX idx_trades_account_date ON account_trades (account_id, trade_date);
CREATE INDEX idx_trades_symbol ON account_trades (symbol);

CREATE TABLE account_holdings (
    id               VARCHAR(36)    NOT NULL,
    version          BIGINT         NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    created_by       VARCHAR(64),
    updated_by       VARCHAR(64),
    account_id       VARCHAR(36)    NOT NULL,
    symbol           VARCHAR(20)    NOT NULL,
    quantity         INT            NOT NULL,
    average_price    DECIMAL(19, 4) NOT NULL,
    total_cost       DECIMAL(19, 2) NOT NULL,
    realized_profit  DECIMAL(19, 2) NOT NULL,
    CONSTRAINT pk_account_holdings PRIMARY KEY (id),
    CONSTRAINT uk_holdings_account_symbol UNIQUE (account_id, symbol),
    CONSTRAINT fk_holdings_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id),
    CONSTRAINT ck_holdings_quantity CHECK (quantity >= 0)
) ENGINE = InnoDB;

CREATE TABLE fee_master (
    id                VARCHAR(36)    NOT NULL,
    version           BIGINT         NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        VARCHAR(64),
    updated_by        VARCHAR(64),
    code              VARCHAR(50)    NOT NULL,
    fee_type          VARCHAR(20)    NOT NULL,
    calculation_type  VARCHAR(20)    NOT NULL,
    fee_value         DECIMAL(19, 6) NOT NULL,
    min_amount        DECIMAL(19, 2),
    max_amount        DECIMAL(19, 2),
    active            BIT(1)         NOT NULL,
    description       VARCHAR(255),
    CONSTRAINT pk_fee_master PRIMARY KEY (id),
    CONSTRAINT uk_fee_master_code UNIQUE (code)
) ENGINE = InnoDB;

CREATE INDEX idx_fee_master_type_active ON fee_master (fee_type, active);

CREATE TABLE tax_master (
    id                VARCHAR(36)    NOT NULL,
    version           BIGINT         NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        VARCHAR(64),
    updated_by        VARCHAR(64),
    code              VARCHAR(50)    NOT NULL,
    tax_type          VARCHAR(30)    NOT NULL,
    calculation_type  VARCHAR(20)    NOT NULL,
    rate              DECIMAL(19, 6) NOT NULL,
    applies_to_side   VARCHAR(10),
    active            BIT(1)         NOT NULL,
    description       VARCHAR(255),
    CONSTRAINT pk_tax_master PRIMARY KEY (id),
    CONSTRAINT uk_tax_master_code UNIQUE (code)
) ENGINE = InnoDB;

CREATE INDEX idx_tax_master_type_active ON tax_master (tax_type, active);

CREATE TABLE account_dividends (
    id                  VARCHAR(36)    NOT NULL,
    version             BIGINT         NOT NULL,
    created_at          DATETIME(6)    NOT NULL,
    updated_at          DATETIME(6)    NOT NULL,
    created_by          VARCHAR(64),
    updated_by          VARCHAR(64),
    account_id          VARCHAR(36)    NOT NULL,
    symbol              VARCHAR(20)    NOT NULL,
    quantity            INT            NOT NULL,
    dividend_per_share  DECIMAL(19, 4) NOT NULL,
    gross_amount        DECIMAL(19, 2) NOT NULL,
    tax_amount          DECIMAL(19, 2) NOT NULL,
    net_amount          DECIMAL(19, 2) NOT NULL,
    payment_date        DATE           NOT NULL,
    CONSTRAINT pk_account_dividends PRIMARY KEY (id),
    CONSTRAINT fk_dividends_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id)
) ENGINE = InnoDB;

CREATE INDEX idx_dividends_account_date ON account_dividends (account_id, payment_date);
CREATE INDEX idx_dividends_symbol ON account_dividends (symbol);

CREATE TABLE account_fees (
    id                   VARCHAR(36)    NOT NULL,
    version              BIGINT         NOT NULL,
    created_at           DATETIME(6)    NOT NULL,
    updated_at           DATETIME(6)    NOT NULL,
    created_by           VARCHAR(64),
    updated_by           VARCHAR(64),
    account_id           VARCHAR(36)    NOT NULL,
    fee_master_id        VARCHAR(36)    NOT NULL,
    trade_id             VARCHAR(36),
    fund_transaction_id  VARCHAR(36),
    fee_type             VARCHAR(20)    NOT NULL,
    base_amount          DECIMAL(19, 2) NOT NULL,
    fee_amount           DECIMAL(19, 2) NOT NULL,
    charged_at           DATETIME(6)    NOT NULL,
    CONSTRAINT pk_account_fees PRIMARY KEY (id),
    CONSTRAINT fk_account_fees_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id),
    CONSTRAINT fk_account_fees_master FOREIGN KEY (fee_master_id) REFERENCES fee_master (id),
    CONSTRAINT fk_account_fees_trade FOREIGN KEY (trade_id) REFERENCES account_trades (id),
    CONSTRAINT fk_account_fees_fund_tx FOREIGN KEY (fund_transaction_id) REFERENCES account_fund_transactions (id)
) ENGINE = InnoDB;

CREATE INDEX idx_account_fees_account_date ON account_fees (account_id, charged_at);

CREATE TABLE account_taxes (
    id              VARCHAR(36)    NOT NULL,
    version         BIGINT         NOT NULL,
    created_at      DATETIME(6)    NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    created_by      VARCHAR(64),
    updated_by      VARCHAR(64),
    account_id      VARCHAR(36)    NOT NULL,
    tax_master_id   VARCHAR(36)    NOT NULL,
    trade_id        VARCHAR(36),
    dividend_id     VARCHAR(36),
    tax_type        VARCHAR(30)    NOT NULL,
    taxable_amount  DECIMAL(19, 2) NOT NULL,
    tax_amount      DECIMAL(19, 2) NOT NULL,
    charged_at      DATETIME(6)    NOT NULL,
    CONSTRAINT pk_account_taxes PRIMARY KEY (id),
    CONSTRAINT fk_account_taxes_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id),
    CONSTRAINT fk_account_taxes_master FOREIGN KEY (tax_master_id) REFERENCES tax_master (id),
    CONSTRAINT fk_account_taxes_trade FOREIGN KEY (trade_id) REFERENCES account_trades (id),
    CONSTRAINT fk_account_taxes_dividend FOREIGN KEY (dividend_id) REFERENCES account_dividends (id)
) ENGINE = InnoDB;

CREATE INDEX idx_account_taxes_account_date ON account_taxes (account_id, charged_at);

CREATE TABLE account_interest_transactions (
    id               VARCHAR(36)    NOT NULL,
    version          BIGINT         NOT NULL,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    created_by       VARCHAR(64),
    updated_by       VARCHAR(64),
    account_id       VARCHAR(36)    NOT NULL,
    principal_amount DECIMAL(19, 2) NOT NULL,
    annual_rate      DECIMAL(10, 6) NOT NULL,
    days             INT            NOT NULL,
    interest_amount  DECIMAL(19, 2) NOT NULL,
    period_start     DATE           NOT NULL,
    period_end       DATE           NOT NULL,
    posted_at        DATETIME(6)    NOT NULL,
    CONSTRAINT pk_account_interest_transactions PRIMARY KEY (id),
    CONSTRAINT fk_interest_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id)
) ENGINE = InnoDB;

CREATE INDEX idx_interest_account_posted ON account_interest_transactions (account_id, posted_at);

CREATE TABLE account_audit_logs (
    id            VARCHAR(36)  NOT NULL,
    version       BIGINT       NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    created_by    VARCHAR(64),
    updated_by    VARCHAR(64),
    entity_type   VARCHAR(50)  NOT NULL,
    entity_id     VARCHAR(36)  NOT NULL,
    action        VARCHAR(20)  NOT NULL,
    performed_by  VARCHAR(64)  NOT NULL,
    details       TEXT,
    CONSTRAINT pk_account_audit_logs PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE INDEX idx_audit_entity ON account_audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_performed_by ON account_audit_logs (performed_by);
