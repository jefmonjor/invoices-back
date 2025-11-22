-- Add VeriFactu extended fields to invoices table
ALTER TABLE invoices ADD COLUMN document_json TEXT;
ALTER TABLE invoices ADD COLUMN document_hash VARCHAR(64);  -- SHA256 hash
ALTER TABLE invoices ADD COLUMN pdf_server_path VARCHAR(500);
ALTER TABLE invoices ADD COLUMN verifactu_tx_id VARCHAR(255);
ALTER TABLE invoices ADD COLUMN pdf_is_final BOOLEAN DEFAULT FALSE;

