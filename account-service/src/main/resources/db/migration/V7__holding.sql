-- =====================================================================
-- v2: holdings with fractional quantities and a stored valuation snapshot
-- (current price, market value, unrealized P&L), refreshed whenever live
-- prices are available.
--
-- Dividends store the quantity held, so that column becomes fractional too.
-- =====================================================================

ALTER TABLE account_holdings
    MODIFY COLUMN quantity      DECIMAL(19, 8) NOT NULL,
    MODIFY COLUMN average_price DECIMAL(19, 8) NOT NULL,
    ADD COLUMN user_id          VARCHAR(64)    NULL AFTER account_id,
    ADD COLUMN current_price    DECIMAL(19, 8) NULL,
    ADD COLUMN market_value     DECIMAL(19, 2) NULL,
    ADD COLUMN unrealized_pnl   DECIMAL(19, 2) NULL,
    ADD COLUMN price_updated_at DATETIME(6)    NULL;

UPDATE account_holdings h
    JOIN investment_accounts a ON a.id = h.account_id
SET h.user_id = a.user_id;

ALTER TABLE account_holdings
    MODIFY COLUMN user_id VARCHAR(64) NOT NULL;

CREATE INDEX idx_holdings_user ON account_holdings (user_id);

ALTER TABLE account_dividends
    MODIFY COLUMN quantity DECIMAL(19, 8) NOT NULL;
