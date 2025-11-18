-- Crear tabla de empresas (emisor)
CREATE TABLE companies (
    id BIGSERIAL PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(500),
    city VARCHAR(100),
    postal_code VARCHAR(10),
    province VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    iban VARCHAR(34),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Crear tabla de clientes
CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    tax_id VARCHAR(20) NOT NULL UNIQUE,
    address VARCHAR(500),
    city VARCHAR(100),
    postal_code VARCHAR(10),
    province VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Modificar tabla invoices - añadir company_id y actualizar estructura
ALTER TABLE invoices DROP COLUMN IF EXISTS client_email;
ALTER TABLE invoices DROP COLUMN IF EXISTS invoice_date;
ALTER TABLE invoices DROP COLUMN IF EXISTS due_date;
ALTER TABLE invoices DROP COLUMN IF EXISTS subtotal;
ALTER TABLE invoices DROP COLUMN IF EXISTS tax;

-- Añadir nuevas columnas si no existen
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS company_id BIGINT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS issue_date TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS base_amount DECIMAL(10,2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS irpf_percentage DECIMAL(5,2) DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS irpf_amount DECIMAL(10,2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS re_percentage DECIMAL(5,2) DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS re_amount DECIMAL(10,2);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS total_amount DECIMAL(10,2);

-- Modificar tabla invoice_items - añadir campos faltantes
ALTER TABLE invoice_items DROP COLUMN IF EXISTS quantity;
ALTER TABLE invoice_items DROP COLUMN IF EXISTS unit_price;

ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS units INTEGER NOT NULL DEFAULT 1;
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS price DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS vat_percentage DECIMAL(5,2) NOT NULL DEFAULT 21;
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS discount_percentage DECIMAL(5,2) DEFAULT 0;
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS subtotal DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Crear índices para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_companies_tax_id ON companies(tax_id);
CREATE INDEX IF NOT EXISTS idx_clients_tax_id ON clients(tax_id);
CREATE INDEX IF NOT EXISTS idx_invoices_company_id ON invoices(company_id);

-- Insertar datos de ejemplo
INSERT INTO companies (id, business_name, tax_id, address, city, postal_code, province, phone, email, iban)
VALUES (
    1,
    'TRANSOLIDO S.L.',
    'B91923755',
    'Castillo Lastrucci, 3, 3D',
    'DOS HERMANAS',
    '41701',
    'SEVILLA',
    '659889201',
    'contacto@transolido.es',
    'ES60 0182 4840 0022 0165 7539'
) ON CONFLICT (tax_id) DO NOTHING;

INSERT INTO clients (id, business_name, tax_id, address, city, postal_code, province, phone, email)
VALUES (
    1,
    'SERSFRITRUCKS, S.A.',
    'A50008588',
    'JIMÉNEZ DE LA ESPADA, 57, BAJO',
    'CARTAGENA',
    '30203',
    'MURCIA',
    '968123456',
    'info@sersfritrucks.com'
) ON CONFLICT (tax_id) DO NOTHING;

-- Actualizar factura de ejemplo para usar company_id
UPDATE invoices SET company_id = 1 WHERE id = 1 AND company_id IS NULL;

-- Limpiar y actualizar items de ejemplo
DELETE FROM invoice_items;
INSERT INTO invoice_items (invoice_id, description, units, price, vat_percentage, discount_percentage, subtotal, total)
VALUES
    (1, '6524LDS Expediente 23', 3, 15.00, 21.00, 0.00, 45.00, 54.45),
    (1, '019KJL Expediente 23', 1, 4021.56, 21.00, 6.25, 3770.21, 4561.55);
