INSERT INTO users (
    email,
    password,
    first_name,
    last_name,
    enabled,
    account_non_expired,
    account_non_locked,
    credentials_non_expired,
    created_at,
    platform_role
) VALUES (
    'admin@admin.com',
    '$2a$10$NDXle74aLx9efSUXokTYU.dye4ywg3YgH2e9cXne9w0RabH32mp8S',
    'Platform',
    'Admin',
    true,
    true,
    true,
    true,
    CURRENT_TIMESTAMP,
    'PLATFORM_ADMIN'
) ON CONFLICT (email) DO NOTHING;
