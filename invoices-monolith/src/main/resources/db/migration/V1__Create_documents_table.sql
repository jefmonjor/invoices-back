CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    minio_object_name VARCHAR(255) NOT NULL UNIQUE,
    invoice_id BIGINT,
    uploaded_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_invoice_id ON documents(invoice_id);
CREATE INDEX idx_documents_minio_object ON documents(minio_object_name);
