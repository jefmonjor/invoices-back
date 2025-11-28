-- V15: Create password_reset_tokens table
-- Author: Authentication Enhancement
-- Date: 2025-11-28
-- Description: Add password reset token functionality

CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    token UUID NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_password_reset_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE
);

-- Index for fast token lookup
CREATE INDEX idx_password_reset_token ON password_reset_tokens(token);

-- Index for cleanup queries (find expired tokens)
CREATE INDEX idx_password_reset_expires ON password_reset_tokens(expires_at);

-- Index for user lookup
CREATE INDEX idx_password_reset_user_id ON password_reset_tokens(user_id);

-- Comment on table
COMMENT ON TABLE password_reset_tokens IS 'Stores temporary tokens for password reset functionality';
COMMENT ON COLUMN password_reset_tokens.token IS 'UUID token sent to user via email';
COMMENT ON COLUMN password_reset_tokens.expires_at IS 'Token expiration timestamp (typically 1 hour from creation)';
COMMENT ON COLUMN password_reset_tokens.used IS 'Flag to prevent token reuse';
