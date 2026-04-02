CREATE TABLE IF NOT EXISTS iot_device_property_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_key VARCHAR(64) NOT NULL,
    device_name VARCHAR(64) NOT NULL,
    property_identifier VARCHAR(128) NOT NULL,
    property_value TEXT,
    trace_id VARCHAR(64),
    message_id VARCHAR(64),
    topic VARCHAR(255),
    device_timestamp BIGINT,
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_iot_prop_record_lookup (tenant_id, product_key, device_name, created_at),
    KEY idx_iot_prop_record_trace (tenant_id, trace_id)
);

CREATE TABLE IF NOT EXISTS iot_device_event_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_key VARCHAR(64) NOT NULL,
    device_name VARCHAR(64) NOT NULL,
    event_name VARCHAR(128) NOT NULL,
    trace_id VARCHAR(64),
    message_id VARCHAR(64),
    topic VARCHAR(255),
    device_timestamp BIGINT,
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_iot_event_record_lookup (tenant_id, product_key, device_name, created_at),
    KEY idx_iot_event_record_trace (tenant_id, trace_id)
);

CREATE TABLE IF NOT EXISTS iot_device_service_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_key VARCHAR(64) NOT NULL,
    device_name VARCHAR(64) NOT NULL,
    service_name VARCHAR(128) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    trace_id VARCHAR(64),
    message_id VARCHAR(64),
    topic VARCHAR(255),
    device_timestamp BIGINT,
    payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_iot_service_record_lookup (tenant_id, product_key, device_name, created_at),
    KEY idx_iot_service_record_trace (tenant_id, trace_id)
);
