ALTER TABLE iot_product
    ADD COLUMN device_type VARCHAR(64) NOT NULL DEFAULT 'DIRECT_DEVICE' AFTER protocol_type;

CREATE INDEX idx_iot_product_device_type ON iot_product(device_type);
