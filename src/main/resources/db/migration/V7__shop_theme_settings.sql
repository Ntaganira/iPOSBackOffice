--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V7__shop_theme_settings.sql
-- Date      : 2026. 10. 26.
-- User      : Heritier NTAGANIRA
-- Desc      : Persists the active shop palette.
--

ALTER TABLE shop_settings
    ADD COLUMN theme_primary VARCHAR(7) NOT NULL DEFAULT '#268071',
    ADD COLUMN theme_secondary VARCHAR(7) NOT NULL DEFAULT '#4FB8A8',
    ADD COLUMN theme_background VARCHAR(7) NOT NULL DEFAULT '#FFFFFF',
    ADD COLUMN theme_surface VARCHAR(7) NOT NULL DEFAULT '#FFFFFF';
