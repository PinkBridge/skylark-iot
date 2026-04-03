ALTER TABLE iot_device
    ADD COLUMN address VARCHAR(512) NULL AFTER device_name;
