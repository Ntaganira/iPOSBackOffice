CREATE TABLE storage_locations (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    location_type VARCHAR(20) NOT NULL DEFAULT 'RACK'
        CHECK (location_type IN ('RACK', 'DRAWER', 'SHELF', 'ROOM', 'OTHER')),
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_at TIMESTAMP WITH TIME ZONE,
    updated_by VARCHAR(50)
);

CREATE INDEX idx_storage_locations_active ON storage_locations (active);

ALTER TABLE shop_products ADD COLUMN location_id BIGINT REFERENCES storage_locations(id);

CREATE INDEX idx_shop_products_location ON shop_products (location_id);

INSERT INTO permissions (code, name, category, sort_order) VALUES
    ('LOCATION_MANAGE', 'Manage storage locations', 'Stock', 540);

INSERT INTO role_permissions (role_code, permission_code) VALUES
    ('ADMIN', 'LOCATION_MANAGE'),
    ('MANAGER', 'LOCATION_MANAGE'),
    ('STOREKEEPER', 'LOCATION_MANAGE');
