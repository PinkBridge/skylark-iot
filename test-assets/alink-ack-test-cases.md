# Alink JSON 回执类联调用例汇总

本文汇总 **属性上报回执**、**属性设置（下行）与设备侧设置回执**、**事件上报回执**、**服务下发与设备回复后的平台回执** 的 MQTTX / Postman 步骤。  
约定设备：`productKey=pk001`，`deviceName=demo-001`，认证与 ACL 请先按 `test-assets/sql/seed-alink-e2e.sql` 初始化并保证 `IOT_ACCESS_ACL_USE_DB` 等配置一致。

**部署提醒**：改代码后需执行 `mvn -DskipTests package` → `docker compose up -d --build iot-access`，见 `iot/readme.md`。

---

## 公共前提

| 项 | 说明 |
|----|------|
| MQTT | `127.0.0.1:1883`，用户名 `demo-001`，密码 `demo-001-secret` |
| EMQX 规则 | 将 `/sys/+/+/thing/#` 上行转发到 `http://iot-access:8089/api/access/upstream`，Body：`{"topic":"${topic}","timestamp":${timestamp},"payload":${payload}}`（SQL 需选出 `topic,payload,timestamp`） |
| 下行 API | `POST http://localhost:8089/api/access/downstream/publish`，需配置 `EMQX_API_KEY` / `EMQX_API_SECRET` |

---

## 1. 属性上报回执（property post → post_reply）

**当前 `iot-access`**：解析 `PROPERTY_POST` 后通过 EMQX 下发 `post_reply`（已实现）。

| 步骤 | Topic | 说明 |
|------|--------|------|
| 订阅（设备） | `/sys/pk001/demo-001/thing/event/property/post_reply` | 或通配 `/sys/pk001/demo-001/thing/event/+/post_reply` |
| 发布（设备） | `/sys/pk001/demo-001/thing/event/property/post` | QoS 建议 1 |

**Payload（发布）**：

```json
{"id":"1","version":"1.0","params":{"Temp":25},"method":"thing.event.property.post"}
```

**预期收到（订阅）**：Topic `.../thing/event/property/post_reply`，Payload 含 `code:200`、`method:thing.event.property.post_reply`、`id` 与上行一致。

---

## 2. 属性设置（下行）与属性设置回执（set → set_reply）

**下行**：云端通过 MQTT 将 **设置属性** 发到设备订阅的 service topic（非“上报”通道）。

| 步骤 | Topic | 说明 |
|------|--------|------|
| 订阅（设备） | `/sys/pk001/demo-001/thing/service/property/set` | 接收云端下发的设置命令 |
| 下行（云端） | 同上 | 见下方 Postman / curl |

**下行 Payload 示例**（`DownstreamPublishRequest.payload` 为 **JSON 字符串**，注意转义）：

```json
{"id":"10001","version":"1.0","params":{"Temp":26},"method":"thing.service.property.set"}
```

**Postman / curl 示例**（宿主机调 `iot-access`）：

```http
POST http://localhost:8089/api/access/downstream/publish
Content-Type: application/json

{
  "topic": "/sys/pk001/demo-001/thing/service/property/set",
  "payload": "{\"id\":\"10001\",\"version\":\"1.0\",\"params\":{\"Temp\":26},\"method\":\"thing.service.property.set\"}",
  "qos": 1,
  "retain": false
}
```

**设备侧“设置回执”上报**（设备执行完后发往平台，属 **上行**）：

| 步骤 | Topic | 说明 |
|------|--------|------|
| 发布（设备） | `/sys/pk001/demo-001/thing/service/property/set_reply` | 需在 ACL 中允许 **publish** 该 topic |

**Payload 示例**（与阿里云物模型常见格式对齐，可按你物模型调整）：

```json
{"id":"10001","code":200,"data":{},"method":"thing.service.property.set_reply"}
```

**关于“平台对 set_reply 的二次回执”**：

- 当前 `AlinkJsonProtocolHandler` 仅对 **`/thing/service/{name}/reply`** 形式（如 `reboot/reply`）组装 **`reply_ack`**。
- Topic 为 **`.../thing/service/property/set_reply`** 时，**不会**走现有 `SERVICE_REPLY` + `reply_ack` 规则；若需要对 `set_reply` 也自动回执，需在协议层单独扩展（与 `property/set` 成对）。

**ACL 提示**：除种子脚本外，若单独收紧策略，需包含：`subscribe` `.../thing/service/property/set`，`publish` `.../thing/service/property/set_reply`。

---

## 3. 事件上报回执（event post → post_reply）

**当前 `iot-access`**：解析 `EVENT_POST` 后下发对应 `post_reply`（已实现）。

| 步骤 | Topic | 说明 |
|------|--------|------|
| 订阅（设备） | `/sys/pk001/demo-001/thing/event/Alarm/post_reply` | 事件名 `Alarm` 时 |
| 发布（设备） | `/sys/pk001/demo-001/thing/event/Alarm/post` | |

**Payload（发布）**：

```json
{"id":"2001","version":"1.0","params":{"code":"OVER_TEMP"},"method":"thing.event.Alarm.post"}
```

**预期收到**：Topic `.../thing/event/Alarm/post_reply`，Payload 含 `method:thing.event.Alarm.post_reply`、`id` 一致。

**通配订阅**：`/sys/pk001/demo-001/thing/event/+/post_reply`

---

## 4. 服务下发与“服务回复”的平台回执（invoke → reply → reply_ack）

这里 **“服务下发”** 指云端把 RPC/服务调用发到设备；设备处理后在 **`.../reply`** 上回复；**平台对设备回复再 ACK** 为 `reply_ack`（当前实现已覆盖 **topic 以 `/thing/service/{serviceName}/reply` 结尾** 的上行）。

**重要（和属性/事件回执不同）**：

- **`reply_ack` 不会在「只有云端下行 invoke」时凭空出现**：必须先有一条 **设备 → 平台** 的上行，发到 **`/sys/{pk}/{dn}/thing/service/{serviceId}/reply`**（与下行 `id` 等字段对齐）。
- 联调时没有真实设备时，用 **MQTTX 或自动化脚本模拟** 发布这条 `reply` 即可；脚本 `alink_ack_e2e.py` 的 `service_flow` 就是在收到下行后 **自动模拟** 该上行，再等 `reply_ack`。

| 步骤 | Topic | 说明 |
|------|--------|------|
| 订阅（设备） | `/sys/pk001/demo-001/thing/service/reboot` | 接收服务调用（示例服务名 `reboot`） |
| 下行（云端） | 同上 | Postman 发 `downstream/publish` |
| 订阅（设备，收平台 ACK） | `/sys/pk001/demo-001/thing/service/reboot/reply_ack` | |
| 发布（设备，服务执行结果） | `/sys/pk001/demo-001/thing/service/reboot/reply` | ACL 需允许 publish |

**下行示例**（调用 reboot）：

```json
{
  "topic": "/sys/pk001/demo-001/thing/service/reboot",
  "payload": "{\"id\":\"3001\",\"version\":\"1.0\",\"params\":{},\"method\":\"thing.service.reboot\"}",
  "qos": 1,
  "retain": false
}
```

**设备回复（上行）Payload**：

```json
{"id":"3001","code":200,"data":{"ok":true},"method":"thing.service.reboot.reply"}
```

**预期收到**：Topic `.../thing/service/reboot/reply_ack`，Payload 含 `method:thing.service.reboot.reply_ack`、`id` 一致。

**通配订阅**：`/sys/pk001/demo-001/thing/service/+/reply_ack`

---

## 5. 验收检查清单（日志）

在 `iot-access` 日志中可辅助确认：

| 日志关键字 | 含义 |
|------------|------|
| `iot.upstream.raw` | EMQX 转发到接入层的原始 body |
| `iot.upstream.event` | 统一事件（`protocolType`、`eventType`、`topic`） |
| `iot.upstream.ack.prepare` | 准备下发 Alink 回执 topic |
| `iot.downstream.publish` | 通过 EMQX Management API 发布回执 |

---

## 6. 相关文件

- ACL 与设备种子：`test-assets/sql/seed-alink-e2e.sql`
- Postman 片段：`test-assets/postman/iot-access-alink-e2e.postman_collection.json`
- MQTTX 简要说明：`test-assets/mqttx/alink-e2e-template.md`
