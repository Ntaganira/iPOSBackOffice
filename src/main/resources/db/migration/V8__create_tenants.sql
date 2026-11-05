--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V8__create_tenants.sql
-- Date      : 2026. 11. 04.
-- User      : Heritier NTAGANIRA
-- Desc      : Creates the tenant registry and seeds the system and default tenants.
--

CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(160),
    phone VARCHAR(40),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tenants_active ON tenants (is_active);

INSERT INTO tenants (id, code, name, email, phone, is_active)
VALUES (0, 'SYSTEM', 'Platform System', NULL, NULL, TRUE);

INSERT INTO tenants (id, code, name, email, phone, is_active)
SELECT 1,
       'default',
       COALESCE((SELECT shop_name FROM shop_settings WHERE id = 1), 'Default Shop'),
       (SELECT email FROM shop_settings WHERE id = 1),
       (SELECT phone FROM shop_settings WHERE id = 1),
       TRUE;

SELECT setval(
        pg_get_serial_sequence('tenants', 'id'),
        GREATEST((SELECT COALESCE(MAX(id), 1) FROM tenants), 1),
        TRUE
);
