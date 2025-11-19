-- Add user_id column to invoices table to track which user created each invoice
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS user_id BIGINT;

-- Set existing invoices to be owned by the admin user (id = 1)
-- This is safe since there's only sample data at this point
UPDATE invoices SET user_id = 1 WHERE user_id IS NULL;

-- Make the column NOT NULL after populating existing rows
ALTER TABLE invoices ALTER COLUMN user_id SET NOT NULL;

-- Add foreign key constraint to ensure referential integrity
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Create index for better query performance when filtering by user_id
CREATE INDEX IF NOT EXISTS idx_invoices_user_id ON invoices(user_id);
