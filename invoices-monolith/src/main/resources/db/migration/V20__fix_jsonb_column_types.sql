-- Fix column types to match JPA Entity definitions (TEXT -> JSONB)
-- This resolves Schema-validation errors where Hibernate expects jsonb but finds text

-- 1. audit_logs.event_data
ALTER TABLE audit_logs 
ALTER COLUMN event_data TYPE JSONB 
USING event_data::jsonb;

-- 2. invoices.document_json
-- Ensure column exists first (it was added in V11 but might be missing in some states)
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS document_json TEXT;
ALTER TABLE invoices 
ALTER COLUMN document_json TYPE JSONB 
USING document_json::jsonb;

-- 3. invoices.verifactu_raw_response
-- Ensure column exists first (it was added in V12)
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS verifactu_raw_response TEXT;
ALTER TABLE invoices 
ALTER COLUMN verifactu_raw_response TYPE JSONB 
USING verifactu_raw_response::jsonb;

-- 4. invoices.canonical_json
-- This column was missing in invoices table (it was in invoice_canonical table in V13)
-- Adding it to invoices table to match InvoiceJpaEntity mapping
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS canonical_json JSONB;
-- No need to alter type if we just added it as JSONB, but if it existed as TEXT:
-- ALTER TABLE invoices ALTER COLUMN canonical_json TYPE JSONB USING canonical_json::jsonb;

-- 5. invoices.qr_payload
-- This column was missing
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS qr_payload JSONB;

