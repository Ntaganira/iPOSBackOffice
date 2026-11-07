--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V11__product_aliases.sql
-- Date      : 2026. 11. 06.
-- User      : Heritier NTAGANIRA
-- Desc      : Adds searchable alternative product names.
--

CREATE TABLE shop_product_aliases (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id),
    product_id BIGINT NOT NULL REFERENCES shop_products(id) ON DELETE CASCADE,
    alias VARCHAR(160) NOT NULL,
    CONSTRAINT uk_shop_product_aliases_tenant_product_alias UNIQUE (tenant_id, product_id, alias)
);

CREATE INDEX idx_shop_product_aliases_tenant_alias
    ON shop_product_aliases (tenant_id, LOWER(alias));

CREATE INDEX idx_shop_product_aliases_product
    ON shop_product_aliases (product_id);
