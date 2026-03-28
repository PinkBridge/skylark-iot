# MQTTX Alink E2E Template

This template is for quick manual testing in MQTTX against the current environment.

## Connection

- Host: `127.0.0.1`
- Port: `1883`
- Client ID: `dev-demo-001`
- Username: `demo-001`
- Password: `demo-001-secret`
- SSL/TLS: `off`

## Subscribe Scenarios

### S1: service downlink channel (should allow)

- Topic: `/sys/pk001/demo-001/thing/service/#`

### S2: property post topic (should deny with current ACL)

- Topic: `/sys/pk001/demo-001/thing/event/property/post`

### S3: property ack (should allow)

- Topic: `/sys/pk001/demo-001/thing/event/property/post_reply`

### S4: event ack wildcard (should allow)

- Topic: `/sys/pk001/demo-001/thing/event/+/post_reply`

### S5: service reply ack (should allow)

- Topic: `/sys/pk001/demo-001/thing/service/+/reply_ack`

## Publish Scenarios

### P1: property post (should allow)

- Topic: `/sys/pk001/demo-001/thing/event/property/post`
- Payload:

```json
{"id":"1001","version":"1.0","params":{"Temp":25.3},"method":"thing.event.property.post"}
```

- Expected ACK Topic: `/sys/pk001/demo-001/thing/event/property/post_reply`

### P2: event post (should allow)

- Topic: `/sys/pk001/demo-001/thing/event/Alarm/post`
- Payload:

```json
{"id":"1002","version":"1.0","params":{"code":"OVER_TEMP"},"method":"thing.event.Alarm.post"}
```

- Expected ACK Topic: `/sys/pk001/demo-001/thing/event/Alarm/post_reply`

### P3: service topic publish (should deny by ACL deny rule)

- Topic: `/sys/pk001/demo-001/thing/service/reboot`
- Payload:

```json
{"id":"1003","version":"1.0","params":{},"method":"thing.service.reboot"}
```

### P4: service reply (should allow)

- Topic: `/sys/pk001/demo-001/thing/service/reboot/reply`
- Payload:

```json
{"id":"3001","code":200,"data":{"ok":true},"message":"success"}
```

- Expected ACK Topic: `/sys/pk001/demo-001/thing/service/reboot/reply_ack`

## Expected Upstream Log Keys

In `docker logs -f iot-access`, verify:

- `protocolType=ALINK_JSON`
- `eventType=PROPERTY_POST` / `EVENT_POST` / `SERVICE_REPLY`
- `messageId` equals payload `id`
- `payloadValid=true` for valid JSON payloads
- `ackTopic` is not empty for property/event/service_reply

## Expected ACK Payload Keys

In MQTTX received ACK payload, verify:

- `id` equals original request `id`
- `code` is `200` on parse success, `500` on parse failure
- `method` equals one of:
  - `thing.event.property.post_reply`
  - `thing.event.{eventId}.post_reply`
  - `thing.service.{serviceName}.reply_ack`

