ALTER TABLE storage_locations ADD COLUMN color_accent VARCHAR(7);

ALTER TABLE storage_locations ADD CONSTRAINT chk_storage_locations_color_accent
    CHECK (color_accent IS NULL OR color_accent ~ '^#[0-9A-Fa-f]{6}$');
