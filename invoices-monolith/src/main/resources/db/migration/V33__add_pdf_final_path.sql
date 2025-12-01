-- Add PDF tracking for final signed invoices

-- Path to final PDF with QR code and AEAT signatures
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS pdf_final_path TEXT;

-- Index for efficient lookup of signed PDFs
CREATE INDEX IF NOT EXISTS idx_invoices_pdf_final ON invoices(pdf_final_path) WHERE pdf_final_path IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN invoices.pdf_final_path IS 'Storage path to final PDF with QR code and AEAT CSV acuse';
