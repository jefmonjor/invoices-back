-- Remove old 'total' column from invoices table (from V2 schema)
-- The entity now uses 'total_amount' instead
ALTER TABLE invoices DROP COLUMN IF EXISTS total;

-- Update NULL values to defaults before adding NOT NULL constraints
-- This ensures existing data won't violate the constraints
UPDATE invoices SET base_amount = 0 WHERE base_amount IS NULL;
UPDATE invoices SET irpf_amount = 0 WHERE irpf_amount IS NULL;
UPDATE invoices SET re_amount = 0 WHERE re_amount IS NULL;
UPDATE invoices SET total_amount = 0 WHERE total_amount IS NULL;

-- Now safe to add NOT NULL constraints
ALTER TABLE invoices ALTER COLUMN base_amount SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN irpf_amount SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN re_amount SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN total_amount SET NOT NULL;

-- Ensure company_id is NOT NULL (if there are NULL values, this will fail - needs manual fix)
-- Only add constraint if column doesn't have NULL values
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM invoices WHERE company_id IS NULL) THEN
        ALTER TABLE invoices ALTER COLUMN company_id SET NOT NULL;
    END IF;
END $$;
