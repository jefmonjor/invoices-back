-- Fix column types to match JPA Entity definitions (TEXT -> JSONB)
-- This resolves Schema-validation errors where Hibernate expects jsonb but finds text

-- 1. audit_logs.event_data
-- Handle potential empty strings or invalid JSON by casting to null if needed, 
-- but standard cast is preferred to catch data issues.
-- Assuming data is valid JSON or null.
ALTER TABLE audit_logs 
ALTER COLUMN event_data TYPE JSONB 
USING event_data::jsonb;

-- 2. invoices.document_json
ALTER TABLE invoices 
ALTER COLUMN document_json TYPE JSONB 
USING document_json::jsonb;

-- 3. invoices.verifactu_raw_response
ALTER TABLE invoices 
ALTER COLUMN verifactu_raw_response TYPE JSONB 
USING verifactu_raw_response::jsonb;

-- 4. invoices.canonical_json
ALTER TABLE invoices 
ALTER COLUMN canonical_json TYPE JSONB 
USING canonical_json::jsonb;

-- 5. invoices.qr_payload
ALTER TABLE invoices 
ALTER COLUMN qr_payload TYPE JSONB 
USING qr_payload::jsonb;
