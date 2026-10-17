-- =====================================================================
-- v2: latest performance snapshot per account, recalculated on request.
--   net_profit = gross_profit - total_charges - total_taxes
-- =====================================================================

CREATE TABLE account_performance (
    id                VARCHAR(36)    NOT NULL,
    version           BIGINT         NOT NULL,
    created_at        DATETIME(6)    NOT NULL,
    updated_at        DATETIME(6)    NOT NULL,
    created_by        VARCHAR(64),
    updated_by        VARCHAR(64),
    account_id        VARCHAR(36)    NOT NULL,
    user_id           VARCHAR(64)    NOT NULL,
    gross_profit      DECIMAL(19, 2) NOT NULL,
    realized_profit   DECIMAL(19, 2) NOT NULL,
    unrealized_profit DECIMAL(19, 2) NOT NULL,
    total_charges     DECIMAL(19, 2) NOT NULL,
    total_taxes       DECIMAL(19, 2) NOT NULL,
    net_profit        DECIMAL(19, 2) NOT NULL,
    prices_live       BIT(1)         NOT NULL,
    calculated_at     DATETIME(6)    NOT NULL,
    CONSTRAINT pk_account_performance PRIMARY KEY (id),
    CONSTRAINT uk_performance_account UNIQUE (account_id),
    CONSTRAINT fk_performance_account FOREIGN KEY (account_id) REFERENCES investment_accounts (id)
) ENGINE = InnoDB;

CREATE INDEX idx_performance_user ON account_performance (user_id);
