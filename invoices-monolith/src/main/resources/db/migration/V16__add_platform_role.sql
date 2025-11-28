-- Add platform_role column to users table
ALTER TABLE users 
ADD COLUMN platform_role VARCHAR(50) NOT NULL DEFAULT 'REGULAR_USER';

-- Add audit columns for platform admin promotion
ALTER TABLE users 
ADD COLUMN platform_admin_granted_at TIMESTAMP;

ALTER TABLE users 
ADD COLUMN platform_admin_granted_by BIGINT;

-- Create index for performance
CREATE INDEX idx_users_platform_role ON users(platform_role);

-- Optional: Promote an existing user to PLATFORM_ADMIN if needed
-- UPDATE users SET platform_role = 'PLATFORM_ADMIN' WHERE email = 'admin@example.com';
