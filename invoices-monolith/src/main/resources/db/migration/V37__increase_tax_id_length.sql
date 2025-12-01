-- Aumentar tamaño de tax_id para permitir formatos con espacios/guiones
-- Cambio: VARCHAR(20) -> VARCHAR(50)

ALTER TABLE companies 
    ALTER COLUMN tax_id TYPE VARCHAR(50);

ALTER TABLE clients 
    ALTER COLUMN tax_id TYPE VARCHAR(50);

-- Comentario: Permite CIF/NIF con formato: "B-91923755", "B 91923755", etc.
