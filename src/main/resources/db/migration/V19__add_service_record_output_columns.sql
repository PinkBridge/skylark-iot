ALTER TABLE iot_device_service_record
    ADD COLUMN output_topic VARCHAR(255) NULL AFTER payload,
    ADD COLUMN output_device_timestamp BIGINT NULL AFTER output_topic,
    ADD COLUMN output_payload TEXT NULL AFTER output_device_timestamp;

