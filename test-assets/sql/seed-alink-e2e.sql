-- Usage:
-- docker exec -i iot-mysql8 mysql -uroot -proot skylark_iot < /path/to/seed-alink-e2e.sql

INSERT INTO iot_product(product_key, name, description, status)
VALUES ('pk001', 'Demo Product', 'Alink E2E test product', 'enabled')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO iot_device(product_key, device_name, display_name, secret, status, protocol_type, protocol_version)
VALUES ('pk001', 'demo-001', 'Demo Device', 'demo-001-secret', 'enabled', 'ALINK_JSON', '1.0')
ON DUPLICATE KEY UPDATE
    display_name = VALUES(display_name),
    secret = VALUES(secret),
    status = VALUES(status),
    protocol_type = VALUES(protocol_type),
    protocol_version = VALUES(protocol_version);

DELETE FROM iot_acl_policy
WHERE product_key = 'pk001'
  AND subject_type = 'device'
  AND subject_value = 'demo-001';

INSERT INTO iot_acl_policy(product_key, subject_type, subject_value, action, topic_pattern, effect, priority, enabled, constraints_json)
VALUES
('pk001', 'device', 'demo-001', 'publish', '/sys/pk001/demo-001/thing/event/property/post', 'allow', 100, 1, NULL),
('pk001', 'device', 'demo-001', 'publish', '/sys/pk001/demo-001/thing/event/+/post', 'allow', 100, 1, NULL),
('pk001', 'device', 'demo-001', 'publish', '/sys/pk001/demo-001/thing/service/+/reply', 'allow', 100, 1, NULL),
('pk001', 'device', 'demo-001', 'subscribe', '/sys/pk001/demo-001/thing/event/property/post_reply', 'allow', 100, 1, NULL),
('pk001', 'device', 'demo-001', 'subscribe', '/sys/pk001/demo-001/thing/event/+/post_reply', 'allow', 100, 1, NULL),
('pk001', 'device', 'demo-001', 'subscribe', '/sys/pk001/demo-001/thing/service/+/reply_ack', 'allow', 100, 1, NULL),
('pk001', 'device', 'demo-001', 'subscribe', '/sys/pk001/demo-001/thing/service/#', 'allow', 100, 1, NULL),
('pk001', 'device', 'demo-001', 'publish', '/sys/pk001/demo-001/thing/service/#', 'deny', 200, 1, NULL);

