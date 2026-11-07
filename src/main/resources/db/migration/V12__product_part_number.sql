--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V12__product_part_number.sql
-- Date      : 2026. 11. 06.
-- User      : Heritier NTAGANIRA
-- Desc      : Adds searchable manufacturer or supplier part numbers.
--

ALTER TABLE shop_products
    ADD COLUMN part_number VARCHAR(80);

CREATE INDEX idx_shop_products_tenant_part_number
    ON shop_products (tenant_id, LOWER(part_number));
