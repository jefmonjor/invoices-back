-- Migration V14: Fix invoice uniqueness and add performance indices

-- 1. Fix Invoice Uniqueness Constraint
-- Remove global unique constraint on invoice_number
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_invoice_number_key;

-- Add unique constraint scoped by company
ALTER TABLE invoices ADD CONSTRAINT uk_invoices_number_company UNIQUE (invoice_number, company_id);

-- 2. Add Performance Indices for Multi-Company

-- Invoices: Filter by company and status (Dashboard)
CREATE INDEX IF NOT EXISTS idx_invoices_company_status ON invoices(company_id, status);

-- Invoices: Sort by date within company (Lists)
CREATE INDEX IF NOT EXISTS idx_invoices_company_date ON invoices(company_id, issue_date DESC);

-- Invoices: Search by number within company
CREATE INDEX IF NOT EXISTS idx_invoices_company_number ON invoices(company_id, invoice_number);

-- Clients: Search by name within company (Autocomplete)
CREATE INDEX IF NOT EXISTS idx_clients_company_name ON clients(company_id, business_name);

-- Audit Logs: Filter by company and date (if table exists)
-- CREATE INDEX IF NOT EXISTS idx_audit_company_date ON audit_logs(company_id, created_at DESC);
