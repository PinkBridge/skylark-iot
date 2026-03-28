ALTER TABLE iot_device
    ADD COLUMN protocol_type VARCHAR(32) NOT NULL DEFAULT 'ALINK_JSON',
    ADD COLUMN protocol_version VARCHAR(32) NULL;

CREATE INDEX idx_iot_device_protocol_type ON iot_device(protocol_type);
