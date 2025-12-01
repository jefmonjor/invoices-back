-- Corregir longitud de columnas en tabla companies
-- phone: Aumentar a 255 para soportar encriptación (actualmente parece estar en 20 en la DB)
-- tax_id: Asegurar que esté en 50

ALTER TABLE companies ALTER COLUMN phone TYPE VARCHAR(255);
ALTER TABLE companies ALTER COLUMN tax_id TYPE VARCHAR(50);
