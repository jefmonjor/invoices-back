-- Corregir longitud de columnas en tabla users para soportar encriptación
-- first_name y last_name: Aumentar a 255 (actualmente 100)

ALTER TABLE users ALTER COLUMN first_name TYPE VARCHAR(255);
ALTER TABLE users ALTER COLUMN last_name TYPE VARCHAR(255);
