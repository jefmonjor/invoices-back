-- Crear tabla users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT true,
    account_non_expired BOOLEAN NOT NULL DEFAULT true,
    account_non_locked BOOLEAN NOT NULL DEFAULT true,
    credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login TIMESTAMP
);

-- Crear índice en email para búsquedas rápidas
CREATE INDEX idx_user_email ON users(email);

-- Crear tabla user_roles para almacenar roles (relación @ElementCollection)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role)
);

-- Crear índice en user_id para mejorar performance
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Insertar usuario admin por defecto
-- Email: admin@invoices.com
-- Password: admin123 (BCrypt hash)
INSERT INTO users (id, email, password, first_name, last_name, enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, updated_at)
VALUES (
    1,
    'admin@invoices.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Admin',
    'System',
    true,
    true,
    true,
    true,
    NOW(),
    NOW()
);

-- Asignar roles ADMIN y USER al usuario admin
INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (1, 'ROLE_USER');

-- Resetear la secuencia del ID para que los siguientes usuarios empiecen desde 2
SELECT setval('users_id_seq', 1, true);
