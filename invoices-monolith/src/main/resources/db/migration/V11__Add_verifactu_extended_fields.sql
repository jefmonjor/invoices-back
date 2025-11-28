-- Add VeriFactu extended fields to invoices table
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS document_json TEXT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS document_hash VARCHAR(64);  -- SHA256 hash
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS pdf_server_path VARCHAR(500);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS verifactu_tx_id VARCHAR(255);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS pdf_is_final BOOLEAN DEFAULT FALSE;

