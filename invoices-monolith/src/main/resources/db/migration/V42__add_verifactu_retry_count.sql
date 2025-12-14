-- Add verifactu_retry_count column to track VeriFactu submission retry attempts
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS verifactu_retry_count INTEGER DEFAULT 0;

-- Add index for efficient querying of pending retries
CREATE INDEX IF NOT EXISTS idx_invoices_verifactu_status_retry 
ON invoices(verifactu_status, verifactu_retry_count) 
WHERE verifactu_status IN ('PENDING', 'PROCESSING');

COMMENT ON COLUMN invoices.verifactu_retry_count IS 'Number of VeriFactu submission retry attempts. Max 5 before marking as FAILED.';
