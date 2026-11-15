--
-- Project   : iPOS - Point of Sale, Stock and Back-Office
-- Package   : db.migration
-- File      : V14__spiced_chai_theme.sql
-- Date      : 2026. 11. 08.
-- User      : Heritier NTAGANIRA
-- Desc      : Replaces the default teal palette with Spiced Chai.
--

UPDATE shop_settings
SET theme_primary = '#D47E30',
    theme_secondary = '#8D5A2B',
    theme_background = '#FDFBD4',
    theme_surface = '#FFFEF7'
WHERE theme_primary = '#268071'
  AND theme_secondary = '#4FB8A8'
  AND theme_background = '#FFFFFF'
  AND theme_surface = '#FFFFFF';

ALTER TABLE shop_settings
    ALTER COLUMN theme_primary SET DEFAULT '#D47E30',
    ALTER COLUMN theme_secondary SET DEFAULT '#8D5A2B',
    ALTER COLUMN theme_background SET DEFAULT '#FDFBD4',
    ALTER COLUMN theme_surface SET DEFAULT '#FFFEF7';
