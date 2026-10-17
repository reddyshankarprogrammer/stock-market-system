-- =====================================================================
-- v2: taxes imported from statements and entered with manual trades share
-- the existing account_taxes table. Imported taxes have no tax rule, so
-- tax_master_id becomes optional; source tells the three origins apart.
-- =====================================================================

ALTER TABLE account_taxes
    MODIFY COLUMN tax_master_id VARCHAR(36) NULL,
    ADD COLUMN source       VARCHAR(20)  NOT NULL DEFAULT 'PLATFORM' AFTER tax_type,
    ADD COLUMN statement_id VARCHAR(36)  NULL,
    ADD COLUMN description  VARCHAR(255) NULL,
    ADD CONSTRAINT fk_account_taxes_statement FOREIGN KEY (statement_id) REFERENCES account_statements (id);

CREATE INDEX idx_account_taxes_type ON account_taxes (tax_type);
CREATE INDEX idx_account_taxes_statement ON account_taxes (statement_id);
