-- Add verifactu_error column to invoices table
-- Stores error messages from VeriFactu if invoice submission fails

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS verifactu_error TEXT;

COMMENT ON COLUMN invoices.verifactu_error IS 'Error message from VeriFactu if invoice submission was rejected';
