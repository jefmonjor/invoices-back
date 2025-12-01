-- Create immutable audit tables for compliance and traceability

-- Certificate audit log (track all certificate operations)
CREATE TABLE IF NOT EXISTS certificate_audit_log (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL, -- 'UPLOADED', 'RENEWED', 'REVOKED', 'ACCESSED'
    event_details JSONB,
    performed_by BIGINT REFERENCES users(id), -- User who performed the action
    ip_address VARCHAR(45), -- IPv4 or IPv6
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- No UPDATE or DELETE allowed on audit table (enforce via triggers or app logic)
CREATE INDEX idx_cert_audit_company ON certificate_audit_log(company_id, created_at DESC);
CREATE INDEX idx_cert_audit_event ON certificate_audit_log(event_type);
CREATE INDEX idx_cert_audit_timestamp ON certificate_audit_log(created_at DESC);

COMMENT ON TABLE certificate_audit_log IS 'Immutable audit trail for all certificate-related operations';

-- Terms and conditions acceptance log (legal compliance)
CREATE TABLE IF NOT EXISTS terms_acceptance_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id BIGINT REFERENCES companies(id) ON DELETE SET NULL,
    terms_version VARCHAR(20) NOT NULL, -- e.g., 'v1.0', 'v1.1'
    terms_type VARCHAR(50) NOT NULL, -- 'GENERAL', 'VERIFACTU', 'PRIVACY', 'DATA_PROCESSING'
    accepted BOOLEAN NOT NULL DEFAULT true,
    ip_address VARCHAR(45),
    user_agent TEXT,
    accepted_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL
);

-- Prevent modification of acceptance records
CREATE INDEX idx_terms_user ON terms_acceptance_log(user_id, accepted_at DESC);
CREATE INDEX idx_terms_company ON terms_acceptance_log(company_id, accepted_at DESC);
CREATE INDEX idx_terms_version ON terms_acceptance_log(terms_version, terms_type);

COMMENT ON TABLE terms_acceptance_log IS 'Immutable log of user terms and conditions acceptance';

-- Invoice audit table additional fields (extend existing verifactu_audit from V29)
-- Add more specific fields for invoice audit
ALTER TABLE verifactu_audit ADD COLUMN IF NOT EXISTS user_id BIGINT REFERENCES users(id);
ALTER TABLE verifactu_audit ADD COLUMN IF NOT EXISTS aeat_response JSONB;
ALTER TABLE verifactu_audit ADD COLUMN IF NOT EXISTS error_code VARCHAR(50);
ALTER TABLE verifactu_audit ADD COLUMN IF NOT EXISTS error_message TEXT;

CREATE INDEX IF NOT EXISTS idx_verifactu_audit_user ON verifactu_audit(user_id);
CREATE INDEX IF NOT EXISTS idx_verifactu_audit_error ON verifactu_audit(error_code) WHERE error_code IS NOT NULL;

COMMENT ON COLUMN verifactu_audit.aeat_response IS 'Full AEAT SOAP response for debugging';
COMMENT ON COLUMN verifactu_audit.error_code IS 'AEAT error code if submission failed';
