-- Rollback for V12: Remove verifactu_raw_response column

ALTER TABLE invoices
DROP COLUMN IF EXISTS verifactu_raw_response;
