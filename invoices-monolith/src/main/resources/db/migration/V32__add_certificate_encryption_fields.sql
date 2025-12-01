-- Add certificate encryption and mode management to companies table
-- Companies table acts as multi-tenant (one company = one tenant)

-- Certificate storage (encrypted with application key)
ALTER TABLE companies ADD COLUMN IF NOT EXISTS certificate_encrypted BYTEA;
ALTER TABLE companies ADD COLUMN IF NOT EXISTS certificate_salt VARCHAR(255);

-- Veri*Factu operation mode (sandbox for testing, production for live AEAT)
ALTER TABLE companies ADD COLUMN IF NOT EXISTS verifactu_mode VARCHAR(20) DEFAULT 'sandbox';

-- Add check constraint for valid modes
ALTER TABLE companies ADD CONSTRAINT chk_verifactu_mode 
    CHECK (verifactu_mode IN ('sandbox', 'production'));

-- Index for filtering by mode
CREATE INDEX IF NOT EXISTS idx_companies_verifactu_mode ON companies(verifactu_mode);

-- Comments for documentation
COMMENT ON COLUMN companies.certificate_encrypted IS 'PFX/P12 certificate encrypted with AES-256';
COMMENT ON COLUMN companies.certificate_salt IS 'Salt used for certificate encryption/decryption';
COMMENT ON COLUMN companies.verifactu_mode IS 'Operating mode: sandbox (AEAT test) or production (AEAT live)';
