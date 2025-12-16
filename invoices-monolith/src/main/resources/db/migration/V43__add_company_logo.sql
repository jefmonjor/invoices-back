-- Add logo_url column to companies table for company logos
ALTER TABLE companies ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);

COMMENT ON COLUMN companies.logo_url IS 'URL to company logo image stored in S3/Backblaze';
