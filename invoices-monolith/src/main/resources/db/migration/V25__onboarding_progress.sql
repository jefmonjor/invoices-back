CREATE TABLE onboarding_progress (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT REFERENCES companies(id),
    status VARCHAR(50) DEFAULT 'REGISTERED',
    company_setup_completed BOOLEAN DEFAULT FALSE,
    first_client_created BOOLEAN DEFAULT FALSE,
    first_invoice_created BOOLEAN DEFAULT FALSE,
    tour_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    skipped_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE demo_templates (
    id BIGSERIAL PRIMARY KEY,
    template_type VARCHAR(50), -- 'CLIENT', 'INVOICE'
    template_data JSONB,
    locale VARCHAR(5) DEFAULT 'es'
);
