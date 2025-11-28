-- Add missing country column to clients and companies tables
-- This resolves Schema-validation errors where Hibernate expects country column but it's missing

-- 1. Add country to clients
ALTER TABLE clients ADD COLUMN IF NOT EXISTS country VARCHAR(100);

-- 2. Add country to companies
ALTER TABLE companies ADD COLUMN IF NOT EXISTS country VARCHAR(100);
