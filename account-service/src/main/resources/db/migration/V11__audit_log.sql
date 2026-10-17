-- =====================================================================
-- v2: audit entries keep the request and response payloads (JSON) of the
-- audited call, so an action can be reconstructed exactly.
-- =====================================================================

ALTER TABLE account_audit_logs
    ADD COLUMN request_json  MEDIUMTEXT NULL,
    ADD COLUMN response_json MEDIUMTEXT NULL;

CREATE INDEX idx_audit_created_at ON account_audit_logs (created_at);
