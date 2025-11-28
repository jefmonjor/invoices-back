CREATE TABLE platform_admin_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    admin_email VARCHAR(255) NOT NULL,
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(255),
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_admin_email ON platform_admin_audit_logs(admin_email);
CREATE INDEX idx_audit_target ON platform_admin_audit_logs(target_type, target_id);
