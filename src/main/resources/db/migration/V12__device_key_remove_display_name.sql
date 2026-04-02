-- Device identity: device_key = system id (MQTT username / topic segment), device_name = user label.
-- Legacy rows: device_name was the id; backfill device_key from it before widening device_name for labels.

ALTER TABLE iot_device
    ADD COLUMN device_key VARCHAR(64) NULL AFTER device_name;

UPDATE iot_device SET device_key = device_name WHERE device_key IS NULL;

ALTER TABLE iot_device
    MODIFY COLUMN device_key VARCHAR(64) NOT NULL,
    MODIFY COLUMN device_name VARCHAR(128) NOT NULL;

ALTER TABLE iot_device DROP INDEX uk_iot_device_tenant_pk_dn;

ALTER TABLE iot_device DROP COLUMN display_name;

ALTER TABLE iot_device
    ADD UNIQUE KEY uk_iot_device_tenant_pk_dk (tenant_id, product_key, device_key),
    ADD UNIQUE KEY uk_iot_device_tenant_pk_dn (tenant_id, product_key, device_name);
