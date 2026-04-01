ALTER TABLE iot_product
    ADD COLUMN protocol_type VARCHAR(64) NOT NULL DEFAULT 'MQTT_ALINK_JSON' AFTER description;

CREATE INDEX idx_iot_product_protocol_type ON iot_product(protocol_type);
