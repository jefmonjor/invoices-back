CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    invoice_id BIGINT,
    invoice_number VARCHAR(50),
    client_id BIGINT,
    client_email VARCHAR(255),
    total DECIMAL(10,2),
    status VARCHAR(20),
    event_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_invoice_id ON audit_logs(invoice_id);
CREATE INDEX idx_audit_logs_client_id ON audit_logs(client_id);
CREATE INDEX idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
