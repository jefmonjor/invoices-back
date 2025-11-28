-- Add unique constraint for document_hash scoped by company
-- This ensures that within a company, the document hash is unique.

-- Drop constraint if it exists (to be idempotent)
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS uk_invoices_hash_company;

-- Add the constraint
ALTER TABLE invoices ADD CONSTRAINT uk_invoices_hash_company UNIQUE (document_hash, company_id);
