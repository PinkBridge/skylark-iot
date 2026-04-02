ALTER TABLE iot_device
    ADD COLUMN connect_status VARCHAR(16) NOT NULL DEFAULT 'disconnected' AFTER status;

CREATE INDEX idx_iot_device_connect_status ON iot_device(connect_status);

