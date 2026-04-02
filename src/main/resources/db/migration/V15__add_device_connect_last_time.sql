ALTER TABLE iot_device
    ADD COLUMN last_connected_at TIMESTAMP NULL AFTER connect_status,
    ADD COLUMN last_disconnected_at TIMESTAMP NULL AFTER last_connected_at;

CREATE INDEX idx_iot_device_last_connected_at ON iot_device(last_connected_at);
CREATE INDEX idx_iot_device_last_disconnected_at ON iot_device(last_disconnected_at);

