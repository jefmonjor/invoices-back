-- Fix company_invitations table schema to match JPA Entity
-- The previous V13 migration created a table that doesn't match the backend model.
-- Since the feature is new and likely unused in production, we will recreate the table.

DROP TABLE IF EXISTS company_invitations;

CREATE TABLE company_invitations (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_invitations_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_invitations_token ON company_invitations(token);
CREATE INDEX idx_invitations_company ON company_invitations(company_id);
CREATE INDEX idx_invitations_email ON company_invitations(email);
