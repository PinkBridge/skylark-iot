ALTER TABLE iot_product
    ADD COLUMN cover_image_url VARCHAR(512) NULL AFTER name,
    ADD COLUMN thumbnail_url VARCHAR(512) NULL AFTER cover_image_url;
