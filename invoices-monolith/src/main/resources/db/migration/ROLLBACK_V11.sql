-- Rollback script for V11__Add_verifactu_extended_fields.sql
-- Run this if migration needs to be reversed

ALTER TABLE invoices DROP COLUMN IF EXISTS document_json;
ALTER TABLE invoices DROP COLUMN IF EXISTS document_hash;
ALTER TABLE invoices DROP COLUMN IF EXISTS pdf_server_path;
ALTER TABLE invoices DROP COLUMN IF EXISTS verifactu_tx_id;
ALTER TABLE invoices DROP COLUMN IF EXISTS pdf_is_final;
