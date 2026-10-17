-- =====================================================================
-- v2: trade management for tracking accounts (manual entry and statement
-- import) on the existing account_trades table.
--
-- * quantity becomes fractional (crypto, forex lots); prices get 8 decimals.
-- * Existing rows are platform EXECUTION trades; buy/sell price, brokerage
--   and user_id are backfilled from the data they already hold.
-- =====================================================================

ALTER TABLE account_trades
    MODIFY COLUMN quantity              DECIMAL(19, 8) NOT NULL,
    MODIFY COLUMN price                 DECIMAL(19, 8) NOT NULL,
    MODIFY COLUMN average_cost_at_trade DECIMAL(19, 8) NULL,
    ADD COLUMN user_id           VARCHAR(64)    NULL AFTER account_id,
    ADD COLUMN exchange          VARCHAR(20)    NULL AFTER symbol,
    ADD COLUMN trade_kind        VARCHAR(20)    NOT NULL DEFAULT 'EXECUTION' AFTER side,
    ADD COLUMN source            VARCHAR(20)    NOT NULL DEFAULT 'PLATFORM' AFTER trade_kind,
    ADD COLUMN buy_price         DECIMAL(19, 8) NULL AFTER price,
    ADD COLUMN sell_price        DECIMAL(19, 8) NULL AFTER buy_price,
    ADD COLUMN brokerage         DECIMAL(19, 2) NOT NULL DEFAULT 0 AFTER gross_amount,
    ADD COLUMN other_fee         DECIMAL(19, 2) NOT NULL DEFAULT 0 AFTER brokerage,
    ADD COLUMN statement_id      VARCHAR(36)    NULL,
    ADD COLUMN external_trade_id VARCHAR(100)   NULL,
    ADD COLUMN notes             VARCHAR(500)   NULL;

UPDATE account_trades t
    JOIN investment_accounts a ON a.id = t.account_id
SET t.user_id    = a.user_id,
    t.buy_price  = CASE WHEN t.side = 'BUY' THEN t.price ELSE t.average_cost_at_trade END,
    t.sell_price = CASE WHEN t.side = 'SELL' THEN t.price ELSE NULL END,
    t.brokerage  = COALESCE((SELECT SUM(f.fee_amount) FROM account_fees f
                             WHERE f.trade_id = t.id AND f.fee_type = 'BROKERAGE'), 0),
    t.other_fee  = t.fee_amount - COALESCE((SELECT SUM(f.fee_amount) FROM account_fees f
                                            WHERE f.trade_id = t.id AND f.fee_type = 'BROKERAGE'), 0);

ALTER TABLE account_trades
    MODIFY COLUMN user_id VARCHAR(64) NOT NULL,
    ADD CONSTRAINT fk_trades_statement FOREIGN KEY (statement_id) REFERENCES account_statements (id),
    ADD CONSTRAINT uk_trades_account_external_id UNIQUE (account_id, external_trade_id);

CREATE INDEX idx_trades_user ON account_trades (user_id);
CREATE INDEX idx_trades_statement ON account_trades (statement_id);
