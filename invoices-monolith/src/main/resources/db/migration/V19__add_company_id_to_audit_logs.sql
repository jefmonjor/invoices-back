-- Add missing company_id column to audit_logs table
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS company_id BIGINT;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_audit_logs_company_id ON audit_logs(company_id);
