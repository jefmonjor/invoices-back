-- Create feature flags table
CREATE TABLE IF NOT EXISTS feature_flags (
    id BIGSERIAL PRIMARY KEY,
    feature_name VARCHAR(255) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT false,
    rollout_percentage INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP,
    updated_by VARCHAR(255)
);

-- Create feature flag whitelist table (for company-specific feature enablement)
CREATE TABLE IF NOT EXISTS feature_flag_whitelist (
    feature_flag_id BIGINT NOT NULL REFERENCES feature_flags(id) ON DELETE CASCADE,
    company_id BIGINT NOT NULL,
    PRIMARY KEY (feature_flag_id, company_id)
);

-- Create index for efficient whitelist lookups
CREATE INDEX IF NOT EXISTS idx_feature_flag_whitelist_company ON feature_flag_whitelist(company_id);

-- Insert default feature flags
INSERT INTO feature_flags (feature_name, enabled, rollout_percentage) VALUES
    ('verifactu_integration', true, 100),
    ('pdf_generation', true, 100),
    ('multi_tenant', true, 100)
ON CONFLICT (feature_name) DO NOTHING;
