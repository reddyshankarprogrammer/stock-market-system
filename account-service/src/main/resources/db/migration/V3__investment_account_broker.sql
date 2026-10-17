-- =====================================================================
-- v2: multiple investment accounts per user across brokers.
--
-- Existing accounts become PLATFORM accounts (traded inside this system),
-- so their behaviour does not change.
-- =====================================================================

ALTER TABLE investment_accounts
    ADD COLUMN broker_name VARCHAR(30)  NOT NULL DEFAULT 'PLATFORM' AFTER account_type,
    ADD COLUMN remarks     VARCHAR(500) NULL AFTER currency;

CREATE INDEX idx_investment_accounts_broker ON investment_accounts (broker_name);
CREATE INDEX idx_investment_accounts_user_broker ON investment_accounts (user_id, broker_name);
