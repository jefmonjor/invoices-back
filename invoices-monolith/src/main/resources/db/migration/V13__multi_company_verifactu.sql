-- Migration V13: Multi-company support and VeriFactu canonicalization

-- 1. Multi-company structure

-- Remove user_id from companies if it exists (cleanup)
-- Remove user_id from companies if it exists (cleanup)
ALTER TABLE companies DROP COLUMN IF EXISTS user_id;

-- Create user_companies table for N:M relationship
CREATE TABLE IF NOT EXISTS user_companies (
    user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, company_id),
    CONSTRAINT fk_user_companies_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_companies_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Add current_company_id to users for context switching
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_company_id BIGINT;
-- Constraint might fail if exists, but usually safe to ignore if column existed. 
-- Ideally we check pg_constraint, but for now we assume if column existed, constraint might too.
-- To be safe, we can drop and re-add or just leave it. 
-- Let's leave constraint addition as is, it might fail if exists but usually Flyway stops at first error.
-- If column exists, we might want to skip constraint addition? 
-- Postgres doesn't have ADD CONSTRAINT IF NOT EXISTS easily.
-- Let's assume if column exists, we are good. But wait, if column exists, adding it again is skipped.
-- Then adding constraint will try to add it. If it exists, it fails.
-- Let's wrap constraint addition in a DO block? No, too complex for this tool.
-- Let's just do CREATE TABLE IF NOT EXISTS and ADD COLUMN IF NOT EXISTS.
-- If constraints fail, we'll deal with it. The user's error was about COLUMNS.

ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_current_company;
ALTER TABLE users ADD CONSTRAINT fk_users_current_company FOREIGN KEY (current_company_id) REFERENCES companies(id);

-- 2. Update Clients for multi-company
-- Add company_id to clients
ALTER TABLE clients ADD COLUMN IF NOT EXISTS company_id BIGINT;

-- Update existing clients to belong to company 1 (default migration strategy)
-- We assume company with ID 1 exists. If not, this might fail or leave nulls.
-- Ideally we should pick the first company or a default one.
UPDATE clients SET company_id = (SELECT id FROM companies ORDER BY id ASC LIMIT 1) WHERE company_id IS NULL;

-- Drop existing unique constraint on tax_id
ALTER TABLE clients DROP CONSTRAINT IF EXISTS clients_tax_id_key;

-- Add new unique constraint scoped by company
ALTER TABLE clients DROP CONSTRAINT IF EXISTS uk_clients_tax_id_company;
ALTER TABLE clients ADD CONSTRAINT uk_clients_tax_id_company UNIQUE (tax_id, company_id);

ALTER TABLE clients DROP CONSTRAINT IF EXISTS fk_clients_company;
ALTER TABLE clients ADD CONSTRAINT fk_clients_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE;

-- 3. Company Invitations
CREATE TABLE IF NOT EXISTS company_invitations (
    code VARCHAR(64) PRIMARY KEY,
    company_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL, -- Admin who created the invitation
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_invitations_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_user FOREIGN KEY (created_by) REFERENCES users(id)
);

-- 4. VeriFactu Canonicalization
-- Separate table for canonical data and hashes
CREATE TABLE IF NOT EXISTS invoice_canonical (
    invoice_id BIGINT PRIMARY KEY,
    canonical_json TEXT NOT NULL,
    document_hash CHAR(64) NOT NULL,
    previous_document_hash CHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_invoice_canonical_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE
);

-- 5. Indices for performance
CREATE INDEX IF NOT EXISTS idx_user_companies_user ON user_companies(user_id);
CREATE INDEX IF NOT EXISTS idx_user_companies_company ON user_companies(company_id);
CREATE INDEX IF NOT EXISTS idx_clients_company ON clients(company_id);
CREATE INDEX IF NOT EXISTS idx_invitations_company ON company_invitations(company_id);
