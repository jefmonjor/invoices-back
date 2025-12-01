-- Add tsvector columns for Full-Text Search
ALTER TABLE invoices ADD COLUMN search_vector tsvector;
ALTER TABLE clients ADD COLUMN search_vector tsvector;
ALTER TABLE companies ADD COLUMN search_vector tsvector;

-- Create GIN indices for fast search
CREATE INDEX invoices_search_idx ON invoices USING GIN(search_vector);
CREATE INDEX clients_search_idx ON clients USING GIN(search_vector);
CREATE INDEX companies_search_idx ON companies USING GIN(search_vector);

-- Function to update invoice search vector
CREATE OR REPLACE FUNCTION invoices_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('spanish', COALESCE(NEW.invoice_number, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(NEW.notes, '')), 'B');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Trigger for invoices
CREATE TRIGGER invoices_search_update
BEFORE INSERT OR UPDATE ON invoices
FOR EACH ROW EXECUTE FUNCTION invoices_search_vector_update();

-- Function to update client search vector
CREATE OR REPLACE FUNCTION clients_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('spanish', COALESCE(NEW.business_name, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(NEW.tax_id, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(NEW.email, '')), 'B');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Trigger for clients
CREATE TRIGGER clients_search_update
BEFORE INSERT OR UPDATE ON clients
FOR EACH ROW EXECUTE FUNCTION clients_search_vector_update();

-- Function to update company search vector
CREATE OR REPLACE FUNCTION companies_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('spanish', COALESCE(NEW.business_name, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(NEW.tax_id, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(NEW.email, '')), 'B');
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

-- Trigger for companies
CREATE TRIGGER companies_search_update
BEFORE INSERT OR UPDATE ON companies
FOR EACH ROW EXECUTE FUNCTION companies_search_vector_update();

-- Update existing data
UPDATE invoices SET search_vector =
    setweight(to_tsvector('spanish', COALESCE(invoice_number, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(notes, '')), 'B');

UPDATE clients SET search_vector =
    setweight(to_tsvector('spanish', COALESCE(business_name, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(tax_id, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(email, '')), 'B');

UPDATE companies SET search_vector =
    setweight(to_tsvector('spanish', COALESCE(business_name, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(tax_id, '')), 'A') ||
    setweight(to_tsvector('spanish', COALESCE(email, '')), 'B');
