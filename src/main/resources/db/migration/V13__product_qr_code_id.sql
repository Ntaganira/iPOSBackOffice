--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V13__product_qr_code_id.sql
-- Date      : 2026. 11. 07.
-- User      : Heritier NTAGANIRA
-- Desc      : Adds searchable QR code identifiers.
--

ALTER TABLE shop_products
    ADD COLUMN qr_code_id VARCHAR(80);

CREATE INDEX idx_shop_products_tenant_qr_code_id
    ON shop_products (tenant_id, qr_code_id);
