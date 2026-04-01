ALTER TABLE iot_device
    ADD COLUMN device_type VARCHAR(64) NOT NULL DEFAULT 'DIRECT_DEVICE' AFTER display_name;

CREATE INDEX idx_iot_device_type ON iot_device(device_type);
