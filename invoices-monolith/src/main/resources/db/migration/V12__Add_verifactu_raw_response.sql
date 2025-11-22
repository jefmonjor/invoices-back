-- Migration V12: Add verifactu_raw_response column to invoices table
-- This column stores the complete JSON response from VeriFactu/AEAT


ALTER TABLE invoices
ADD COLUMN verifactu_raw_response TEXT;

COMMENT ON COLUMN invoices.verifactu_raw_response IS 'Complete JSON response from VeriFactu/AEAT (for audit and debugging)';
