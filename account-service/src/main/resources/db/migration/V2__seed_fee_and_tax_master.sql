-- =====================================================================
-- Default fee schedule and tax rules.
--
-- The rates are illustrative defaults for the simulation, not real broker
-- or tax-authority rates. Change them with a new migration (deactivate the
-- old row, insert a new one) so the history of what was charged stays valid.
-- =====================================================================

INSERT INTO fee_master
    (id, version, created_at, updated_at, created_by, updated_by,
     code, fee_type, calculation_type, fee_value, min_amount, max_amount, active, description)
VALUES
    (UUID(), 0, NOW(6), NOW(6), 'flyway', 'flyway',
     'BROKERAGE_STANDARD', 'BROKERAGE', 'PERCENTAGE', 0.100000, 1.00, 25.00, b'1',
     'Broker commission: 0.10% of trade value, min 1.00, max 25.00'),
    (UUID(), 0, NOW(6), NOW(6), 'flyway', 'flyway',
     'PLATFORM_STANDARD', 'PLATFORM', 'FIXED', 0.500000, NULL, NULL, b'1',
     'Platform fee: 0.50 per trade'),
    (UUID(), 0, NOW(6), NOW(6), 'flyway', 'flyway',
     'EXCHANGE_STANDARD', 'EXCHANGE', 'PERCENTAGE', 0.003500, NULL, NULL, b'1',
     'Exchange and clearing levy: 0.0035% of trade value'),
    (UUID(), 0, NOW(6), NOW(6), 'flyway', 'flyway',
     'WITHDRAWAL_STANDARD', 'WITHDRAWAL', 'FIXED', 2.000000, NULL, NULL, b'1',
     'Withdrawal fee: 2.00 per withdrawal');

INSERT INTO tax_master
    (id, version, created_at, updated_at, created_by, updated_by,
     code, tax_type, calculation_type, rate, applies_to_side, active, description)
VALUES
    (UUID(), 0, NOW(6), NOW(6), 'flyway', 'flyway',
     'TRADE_TAX_SELL', 'TRADE_TAX', 'PERCENTAGE', 0.050000, 'SELL', b'1',
     'Transaction tax: 0.05% of SELL trade value'),
    (UUID(), 0, NOW(6), NOW(6), 'flyway', 'flyway',
     'CAPITAL_GAINS_STANDARD', 'CAPITAL_GAINS_TAX', 'PERCENTAGE', 10.000000, NULL, b'1',
     'Capital gains tax: 10% of positive realized profit'),
    (UUID(), 0, NOW(6), NOW(6), 'flyway', 'flyway',
     'DIVIDEND_WITHHOLDING', 'DIVIDEND_TAX', 'PERCENTAGE', 5.000000, NULL, b'1',
     'Dividend withholding tax: 5% of gross dividend');
