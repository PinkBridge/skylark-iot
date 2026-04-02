ALTER TABLE iot_product
    ADD COLUMN product_secret VARCHAR(32) NULL AFTER product_key;
