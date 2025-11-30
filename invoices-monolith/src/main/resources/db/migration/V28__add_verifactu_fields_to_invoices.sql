ALTER TABLE invoices ADD COLUMN IF NOT EXISTS is_rectificativa BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS rectifies_invoice_id BIGINT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS hash VARCHAR(128);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS last_hash_before VARCHAR(128);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS csv_acuse VARCHAR(200);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS qr_data TEXT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS xml_content TEXT;

ALTER TABLE invoices ADD CONSTRAINT fk_invoices_rectifies FOREIGN KEY (rectifies_invoice_id) REFERENCES invoices(id);
CREATE INDEX idx_invoices_hash ON invoices(hash);
