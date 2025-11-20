-- Remove old 'total' column from invoices table (from V2 schema)
-- The entity now uses 'total_amount' instead
ALTER TABLE invoices DROP COLUMN IF EXISTS total;

-- Ensure total_amount is NOT NULL (it should already be set from V5)
ALTER TABLE invoices ALTER COLUMN total_amount SET NOT NULL;

-- Ensure base_amount, irpf_amount, re_amount are NOT NULL
ALTER TABLE invoices ALTER COLUMN base_amount SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN irpf_amount SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN re_amount SET NOT NULL;

-- Ensure company_id is NOT NULL (required field)
ALTER TABLE invoices ALTER COLUMN company_id SET NOT NULL;
