-- Corregir secuencias desincronizadas para TODAS las tablas con BIGSERIAL
-- Esto evita el error "duplicate key value violates unique constraint"

-- Tablas principales
SELECT setval('companies_id_seq', COALESCE((SELECT MAX(id) FROM companies), 1));
SELECT setval('clients_id_seq', COALESCE((SELECT MAX(id) FROM clients), 1));
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1));

-- Tablas de facturación
SELECT setval('invoices_id_seq', COALESCE((SELECT MAX(id) FROM invoices), 1));
SELECT setval('invoice_items_id_seq', COALESCE((SELECT MAX(id) FROM invoice_items), 1));

-- Tablas de documentos y auditoría
SELECT setval('documents_id_seq', COALESCE((SELECT MAX(id) FROM documents), 1));
SELECT setval('verifactu_audit_id_seq', COALESCE((SELECT MAX(id) FROM verifactu_audit), 1));

-- Tablas de auditoría nuevas (si existen)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'certificate_audit_log') THEN
        PERFORM setval('certificate_audit_log_id_seq', COALESCE((SELECT MAX(id) FROM certificate_audit_log), 1));
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'terms_acceptance_log') THEN
        PERFORM setval('terms_acceptance_log_id_seq', COALESCE((SELECT MAX(id) FROM terms_acceptance_log), 1));
    END IF;
END $$;
