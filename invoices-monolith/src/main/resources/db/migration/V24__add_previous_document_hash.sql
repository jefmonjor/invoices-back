-- Add previous_document_hash column to invoices table
-- This column is required for the blockchain-like chaining of invoices (VeriFactu requirement)

ALTER TABLE invoices ADD COLUMN IF NOT EXISTS previous_document_hash VARCHAR(64);

-- Create index for faster lookups when verifying chains
CREATE INDEX IF NOT EXISTS idx_invoices_previous_hash ON invoices(previous_document_hash);
