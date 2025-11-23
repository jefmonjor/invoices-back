-- Migration V13: Add canonical JSON, chain fields, and QR payload for VERI*FACTU

ALTER TABLE invoices ADD COLUMN canonical_json TEXT;
ALTER TABLE invoices ADD COLUMN previous_document_hash CHAR(64);
ALTER TABLE invoices ADD COLUMN qr_payload TEXT;

-- Indices para performance
CREATE INDEX idx_invoices_prev_hash ON invoices(previous_document_hash);

-- Comentarios
COMMENT ON COLUMN invoices.canonical_json IS 'Canonicalized JSON for hash calculation (audit trail)';
COMMENT ON COLUMN invoices.previous_document_hash IS 'Hash of previous invoice for chaining (rectifications/amendments)';
COMMENT ON COLUMN invoices.qr_payload IS 'QR code payload returned by VERI*FACTU (for PDF embedding)';
