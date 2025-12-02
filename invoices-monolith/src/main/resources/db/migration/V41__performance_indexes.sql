-- Índices de rendimiento para optimizar consultas frecuentes

-- 1. Foreign Keys faltantes (para JOINs rápidos)
CREATE INDEX IF NOT EXISTS idx_users_current_company ON users(current_company_id);
CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items(invoice_id);
CREATE INDEX IF NOT EXISTS idx_documents_invoice ON documents(invoice_id);

-- 2. Búsquedas por estado y fechas (para filtros de listados)
CREATE INDEX IF NOT EXISTS idx_invoices_status_date ON invoices(status, issue_date DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_issue_date ON invoices(issue_date DESC);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled);

-- 3. Búsquedas de texto (para autocompletado y filtros)
-- Nota: Para búsquedas complejas ya tenemos tsvector en V36, estos son para coincidencias exactas rápidas
CREATE INDEX IF NOT EXISTS idx_companies_business_name ON companies(business_name);
CREATE INDEX IF NOT EXISTS idx_clients_business_name ON clients(business_name);
CREATE INDEX IF NOT EXISTS idx_users_names ON users(last_name, first_name);

-- 4. Optimizaciones para Veri*Factu
CREATE INDEX IF NOT EXISTS idx_invoices_chain_hash ON invoices(last_hash_before);
CREATE INDEX IF NOT EXISTS idx_companies_verifactu_mode ON companies(verifactu_mode);
