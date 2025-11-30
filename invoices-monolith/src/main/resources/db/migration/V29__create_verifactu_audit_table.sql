CREATE TABLE verifactu_audit (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices(id),
    event VARCHAR(50) NOT NULL,
    details JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE INDEX idx_verifactu_audit_invoice ON verifactu_audit(invoice_id);
