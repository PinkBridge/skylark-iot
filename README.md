# IoT 云平台（讨论纪要与技术方案汇总）

本文档汇总了近期关于 **IoT 云平台** 的模块划分、设备接入边界、OTA 升级、以及 **Spring Boot 2.6.13 + EMQX** 的技术选型讨论结论，便于后续评审与排期。

---

## 1. 术语与范围

- **OTA（Over-The-Air）**：设备通过网络远程下载并更新固件/软件（常见为固件 OTA/FOTA）。
- **一阶段**：在一次链路中完成判定与执行（例如：设备升级任务一次下发；或服务端一次判定并返回结果）。
- **TLS**：传输加密与服务器身份校验（常见为单向 TLS）。
- **mTLS（双向 TLS）**：在 TLS 基础上增加客户端证书，服务器也校验客户端身份（设备也有证书/私钥）。

> 本文聚焦云平台侧，不包含硬件模组研发细节（但会说明与设备端协作的契约）。

---

## 2. 云平台模块划分（四大模块）

我们将云平台拆分为：

1) **设备接入模块**  
2) **设备管理模块**  
3) **应用使能模块**  
4) **数据分析模块**

### 2.1 设备接入模块（Device Access / Connectivity）

**使命**：让设备安全稳定地“连上来”，并将设备侧协议转换为平台内部统一消息模型；将平台下行命令可靠送达设备连接层。

**包含（IN）**

- **连接与通道**：MQTT/HTTP(S) 接入、会话管理、心跳、断线、限流
- **设备身份校验**：密钥/证书/Token 校验；非法连接拒绝；可选 IP 黑白名单
- **入口消息处理**：Topic/路径解析，载荷基本格式校验（语法层）
- **标准化**：设备协议 → 统一内部消息模型（`deviceId + messageType + payload + timestamp`）
- **出站投递**：接收平台下行命令，映射为设备协议并下发（不做业务权限判定）
- **可观测性（接入层）**：连接数、鉴权失败、解析失败、延迟等

**不包含（OUT）**

- 设备档案/归属/绑定关系（设备管理）
- OTA 全流程状态机、灰度、回滚（设备管理）
- 业务规则联动、通知（应用使能）
- 报表与分析（数据分析）
- 用户/租户权限（应用使能 + 设备管理）

### 2.2 设备管理模块（Device Management）

**使命**：设备“是谁、属于谁、什么状态、怎么控、怎么升级”。

- 设备档案：型号、批次、固件版本、绑定关系
- 生命周期：注册/激活/禁用/注销/转移
- 下行控制：命令编排、参数配置、调度
- **OTA 状态机**：版本库、任务下发、进度/结果、失败重试、回滚策略（云端控制面）

### 2.3 应用使能模块（Application Enablement）

**使命**：给小程序/App/第三方系统提供统一可用的业务 API 与编排能力。

- 对外 API、鉴权、限流
- 用户维度业务：我的设备、快捷控制、场景入口
- 轻量规则/自动化（可选二期）
- 与消息触达（模板消息/推送/工单）集成（按业务需要）

### 2.4 数据分析模块（Data Analytics）

**使命**：遥测/事件/升级数据的存储、查询、指标与报表。

- 摄入：消费上行数据与管理事件
- 存储：时序库/冷热分层
- 计算：在线率、OTA 成功率、异常率等
- 输出：报表 API、大屏、导出

---

## 3. IoT OTA 升级：概念与流程

### 3.1 OTA 的典型流程（推荐 A/B 双分区）

1. 云端发布新版本（版本号、大小、hash、签名、适配机型）
2. 设备上报当前版本与能力（或云端主动下发任务）
3. 云端下发下载信息（URL/分片清单/校验信息）与任务策略（灰度/重试/窗口期）
4. 设备下载（断点续传），写入 **非运行分区**（A/B）
5. 校验完整性（hash）与真实性（签名验签）
6. 切换启动分区并重启
7. 自检失败则回滚到旧分区
8. 上报升级结果（成功/失败原因）

### 3.2 为什么固件要分段/分片

分片的核心目的：**弱网与资源受限下提高成功率**。

- 网络不稳：失败只重下失败片段，不用整包重来
- 设备资源小：可边下边写，降低 RAM/存储峰值
- 可观测：定位失败片段更容易

> 不分片也可通过 **HTTP Range** 实现“逻辑分片”，即一个整包 URL 但按区间下载。

### 3.3 什么是分片 URL

**分片 URL**：一个固件包被拆为多个片段后，每个片段对应一个下载地址。例如：

- `.../fw/v1.2.3/chunk/0`
- `.../fw/v1.2.3/chunk/1`
- ...

通常会配套：

- 每片 hash + 整包 hash
- URL 时效签名（防盗链）
- 失败重试策略

### 3.4 分片由谁来做

不要求硬件工程师手工切片。常见方式：

- **构建/发布流水线自动切片**：CI 打包后切片 + 生成 manifest（推荐）
- **云平台上传时自动切片**：平台内部完成切片与清单生成
- **不切物理分片，使用 HTTP Range**：由设备按区间拉取

---

## 4. 连接选型：EMQX

我们确定连接采用 **EMQX（MQTT Broker）**。

推荐对接模式（优先级从高到低）：

1. **EMQX 规则引擎/桥接** → Kafka/RabbitMQ/HTTP Webhook → Spring Boot 消费（更解耦）
2. Spring Boot 作为 MQTT 客户端订阅 Topic 直接消费（量小可行）

---

## 5. TLS 如何挂到 EMQX（概念方案）

两种主流方式：

### 5.1 EMQX 直接终结 TLS（mqtts）

- EMQX 开启 SSL Listener（常见端口 **8883**）
- 配置服务端证书与私钥
- 设备以 `mqtts://domain:8883` 连接并校验服务器证书

### 5.2 负载均衡/Nginx 终结 TLS

- 对外由 LB/Nginx 终结 TLS
- 内网转发到 EMQX（可明文或二次 TLS）

---

## 6. 设备端也要证书吗？（结论）

**不一定**。需要区分：

- **服务端证书**：云端（EMQX/LB）必配，用于设备校验服务器身份（强烈建议）
- **客户端证书**：每台设备一张，用于 **mTLS（双向 TLS）**，可选

### 6.1 什么是双向 TLS（mTLS）

在单向 TLS（客户端校验服务器）基础上增加：

- **服务器也校验客户端证书**  
设备需持有客户端证书与私钥（通常出厂烧录），用于更强的身份认证与接入控制。

---

## 7. 技术选型约束（与现有服务对齐）

我们决定继续沿用现有后端技术栈：

- **Spring Boot 2.6.13**
- **Java 8**

> 该结论来自现有服务 `xiaolvxingqiu-boot` 的 `spring-boot-starter-parent` 版本与 `java.version`。

---

## 8. 下一步（建议落地清单）

若要进入可执行排期，建议先补齐以下“拍板项”：

1. **EMQX 版本**：4.x 或 5.x（影响配置项与插件）
2. **鉴权方案一期**：TLS + 用户名密码/Token（推荐） vs mTLS（高安全）
3. **Topic 规范**：`/{productId}/{deviceId}/{messageType}`（示例），以及 ACL 规则
4. **上行消息模型**：统一字段（deviceId、timestamp、payload、seq）
5. **OTA 形态**：整包 URL + Range vs 分片 + manifest；是否 A/B 分区（设备端）

---

## 9. 附：可用于评审的一句话总结

我们将 IoT 云平台拆为 **接入/管理/使能/分析** 四模块；接入层以 **EMQX** 承载连接与可靠投递，业务用 **Spring Boot 2.6.13(Java 8)** 实现管理与应用；安全上先落地 **TLS（服务端证书）**，设备端证书（mTLS）作为可选升级；OTA 采用可回滚设计，分片/Range 用于弱网下提升成功率。

---

## 10. 设备接入模块 MVP（已落地骨架）

目录：`iot/`

### 10.1 运行方式

> 约定：**任何代码改动要生效，都必须重启 `iot-access`**（本地进程重启或容器重启）。
> 否则你看到的行为仍然是旧版本。

```bash
cd iot && mvn spring-boot:run
```

默认端口：`8089`

#### 10.1.1 改完代码后如何重启

- **本地运行（mvn spring-boot:run）**：停止当前进程后重新执行上面的启动命令。
- **docker compose 运行**：仅 `restart` 有时不会更新镜像（你改了代码但容器仍在跑旧版本），所以统一按以下 **3 步**做：

```bash
# 1) 打包（生成最新 jar）
mvn -DskipTests package

# 2) 重建镜像并重启（确保容器使用最新 jar）
docker compose up -d --build iot-access

# 3) 健康检查（确认服务已起来）
curl http://localhost:8089/actuator/health
```

### 10.1.1 数据库（设备管理模块：MySQL）

设备管理模块（`mgmt`）使用 **MySQL + Flyway + MyBatis**。

默认读取环境变量（可按需覆盖）：

- `DB_URL`（默认：`jdbc:mysql://localhost:3306/skylark_iot?...`）
- `DB_USERNAME`（默认：`root`）
- `DB_PASSWORD`（默认：`root`）
- `DB_DRIVER`（默认：`com.mysql.cj.jdbc.Driver`）

本机快速启动 MySQL（如你本地已能拉取镜像）：

```bash
docker run -d --name iot-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=skylark_iot -p 3306:3306 mysql:8
```

> 启动服务后 Flyway 会自动执行 `db/migration/V1__init_device_mgmt.sql` 建表。

### 10.2 提供的接口

- `POST /api/access/emqx/auth`：EMQX HTTP Auth（用户名/密码鉴权）
- `POST /api/access/emqx/acl`：EMQX HTTP ACL（publish/subscribe topic 判定）
- `POST /api/access/upstream`：上行消息接收（MVP 先结构化日志落地）

设备管理（mgmt）接口（P0）：

- 产品：`POST /api/mgmt/products`、`GET /api/mgmt/products`、`GET /api/mgmt/products/{productKey}`、`PUT /api/mgmt/products/{productKey}`、`PATCH /api/mgmt/products/{productKey}/enable|disable`
- 设备：`POST /api/mgmt/products/{productKey}/devices`、`GET /api/mgmt/products/{productKey}/devices`、`GET /api/mgmt/products/{productKey}/devices/{deviceName}`、`PUT /api/mgmt/products/{productKey}/devices/{deviceName}`、`PATCH /api/mgmt/products/{productKey}/devices/{deviceName}/enable|disable`、`POST /api/mgmt/products/{productKey}/devices/{deviceName}/reset-secret`
- 物模型：`PUT /api/mgmt/products/{productKey}/thing-model`、`GET /api/mgmt/products/{productKey}/thing-model`

### 10.3 MVP 配置项

见 `iot/src/main/resources/application.yml`：

- `iot.access.devices[].username/password`：设备鉴权凭据
- `iot.access.devices[].publish-prefix`：允许上行 Topic 前缀
- `iot.access.devices[].subscribe-prefix`：允许订阅 Topic 前缀
- `iot.access.auth.use-db`：`/auth` 是否使用数据库（默认 `true`）
- `iot.access.acl.use-db`：`/acl` 是否使用数据库策略（默认 `false`，灰度开启）

> 说明：当前已支持“静态配置 + 数据库”双模式，可通过开关回滚。

---

### 10.4 上行消息字段说明（与阿里物模型 Topic 对齐）

`POST /api/access/upstream` 建议由 EMQX Rules 将 `topic` 与 `payload` 转发过来。

- `deviceId` / `messageType`：可以不填（后端可从 `topic` 自动解析）
- `topic`：建议必传，用于后端解析 `deviceId` 与分类
- `payload`：MVP 先按字符串透传（便于后续再做结构解析入库）

可选字段：

- `traceId`：链路追踪 ID。若不传，服务端会生成，并在响应头返回 `X-Trace-Id`。

幂等（本版本决定）：

- 本版本 **暂不实现幂等去重**（不引入 Redis/DB 去重表）
- 后续幂等以协议侧 **messageId** 为准
  - 若设备上行采用阿里 **Alink JSON**，可直接使用 payload 中的 `id` 作为 messageId
  - 幂等键建议：`deviceId + id`（重试时 `id` 必须保持不变）

---

### 10.5 内部事件总线（解耦日志/入队/落库）

`iot-access` 收到上行后会先将 `UpstreamIngestRequest` **统一映射**为内部标准事件模型 `DeviceUpstreamEvent`，然后发布到事件总线，由各个 listener 异步处理。

- **发布点**：`UpstreamIngestServiceImpl` → `DeviceEventBus.publish(event)`
- **事件包装**：`DeviceUpstreamEventPublished`
- **异步线程池**：`iotEventExecutor`（线程名前缀 `iot-event-`）
- **默认 listener**
  - `UpstreamLoggingListener`：结构化日志（异步）
  - `UpstreamQueueListener`：入队占位（异步，后续接 RabbitMQ/Kafka）
  - `UpstreamStorageListener`：落库占位（异步，后续接 MySQL/TimescaleDB）

> 后续要新增处理（例如：协议解析、数据校验、告警触发），只要再加一个 `@EventListener` listener 即可，不影响主链路。

---

### 10.6 多协议解析层（统一事件模型）

为支持后续多种设备报文格式（不局限 Alink JSON），`iot-access` 新增了可插拔协议处理层（解析 + 回执成对）：

- 协议契约：`ProtocolHandler`（`supports(ctx)` + `parse(ctx)` + `buildAck(result, ctx)`）
- 回执模型：`AckMessage`（`topic/payload/qos/retain`）
- 调度组件：`ProtocolResolver` + `ProtocolParserRegistry`（内部按 handler 选型）
- 已实现处理器：
  - `AlinkJsonProtocolHandler`（同一模块内完成 Alink 解析与 ACK 组包）
  - `UnknownProtocolParser`（兜底，不中断链路）

解析结果统一映射到 `DeviceUpstreamEvent` 后再发布到事件总线，关键统一字段包括：

- `protocolType`（如 `ALINK_JSON`）
- `eventType`（`PROPERTY_POST` / `EVENT_POST` / `SERVICE_REPLY` / `UNKNOWN_PROTOCOL`）
- `messageId`（对齐 Alink `id`）
- `eventName` / `serviceName` / `alinkMethod`
- `payloadValid` / `parseError`

设备协议标识来源优先级：

1. `iot_device.protocol_type`（推荐，设备主数据）
2. Topic 规则推断（如 `/sys/...` 默认推断为 `ALINK_JSON`）
3. 兜底 `UNKNOWN`

接入新协议步骤（示例）：

1. 新增一个 `ProtocolHandler` 实现类（如 `VendorXProtocolHandler`）。
2. 在 `supports(ctx)` 中声明协议匹配条件（建议优先匹配 `iot_device.protocol_type`）。
3. 在 `parse(ctx)` 中产出统一字段（`eventType/messageId/...`）。
4. 在 `buildAck(...)` 中实现同协议回执规则（成功/失败均可回执）。
5. 启动后通过上行日志与 ACK 下发日志校验“解析与回执一致性”。

验收用例（最小）：

- 用 `ALINK_JSON` 设备上报属性：应得到 `eventType=PROPERTY_POST`。
- 用 `ALINK_JSON` 设备上报事件：应得到 `eventType=EVENT_POST`。
- 下发 service 并上报 reply：应得到 `eventType=SERVICE_REPLY` 且 `messageId` 对齐。
- 发送未知格式 payload：应得到 `eventType=UNKNOWN_PROTOCOL`，接口不 500。

---

## 11. EMQX 5.0.26 联调配置汇总（Auth / ACL / Rules）

本节记录我们在本机联调中实际跑通的 EMQX 侧配置要点，避免反复踩坑。

### 11.1 容器与网络（推荐：同一个 compose 网络）

为避免 Windows 环境下 `host.docker.internal`/路由/防火墙导致的 HTTP 回调超时，建议让 EMQX 与 `iot-access` 处于同一个 `docker compose` 网络，并在 EMQX 中使用服务名访问。

- EMQX Dashboard：`http://127.0.0.1:18083`
- iot-access 健康检查：`http://127.0.0.1:8089/actuator/health`

EMQX 回调 URL（容器内访问）：

- Auth：`http://iot-access:8089/api/access/emqx/auth`
- ACL：`http://iot-access:8089/api/access/emqx/acl`
- 上行 Webhook：`http://iot-access:8089/api/access/upstream`
- 会话上下线 Webhook（可选，需配置 `iot.access.webhook`）：`http://iot-access:8089/api/access/emqx/webhook/session`（见 11.6）

### 11.2 Authentication（HTTP Server / password_based）

路径（概念）：Access Control → Authentication → Create → HTTP Server（password_based）

关键点：

- Method：`POST`
- URL：`http://iot-access:8089/api/access/emqx/auth`
- Headers：`Content-Type: application/json`
- Body（示例）：

```json
{
  "clientid": "${clientid}",
  "username": "${username}",
  "password": "${password}"
}
```

> 注意：EMQX 5.x **不兼容** 4.x 那种“只看 HTTP 状态码”的返回。必须返回 JSON（见 11.5）。

### 11.3 Authorization（HTTP Server）

路径（概念）：Access Control → Authorization → Create → HTTP Server

关键点：

- Method：`POST`
- URL：`http://iot-access:8089/api/access/emqx/acl`
- Headers：`Content-Type: application/json`
- Body（示例）：

```json
{
  "clientid": "${clientid}",
  "username": "${username}",
  "action": "${action}",
  "topic": "${topic}"
}
```

ACL 规则约定（MVP）：

- `username == deviceName`
- 允许访问阿里物模型风格 topic：`/sys/{productKey}/{deviceName}/thing/...`

ACL 动态化（可选开关 `iot.access.acl.use-db=true`）：

- 读取策略表：`iot_acl_policy`
- 按 `product_key + action + subject(deviceName)` 拉取候选策略
- 匹配顺序：`priority` 高优先
- 决策规则：**deny 优先于 allow**，无命中默认 deny（fail-close）

策略表示例（MySQL）：

```sql
INSERT INTO iot_acl_policy(product_key, subject_type, subject_value, action, topic_pattern, effect, priority, enabled)
VALUES
('pk001', 'device', 'demo-001', 'publish', '/sys/pk001/demo-001/thing/event/property/post', 'allow', 100, 1),
('pk001', 'device', 'demo-001', 'subscribe', '/sys/pk001/demo-001/thing/service/#', 'allow', 100, 1),
('pk001', 'device', 'demo-001', 'publish', '/sys/pk001/demo-001/thing/service/#', 'deny', 200, 1);
```

### 11.4 Rules（将上行 MQTT 转发到 iot-access）

我们按阿里物模型的常用上行 topic 建规则（可拆多条，便于观察）：

- 属性上报（Alink JSON）：`/sys/+/+/thing/event/property/post`
- 透传 raw：`/sys/+/+/thing/model/up_raw`
- 事件上报（可选）：`/sys/+/+/thing/event/+/post`

SQL（示例）：

```sql
SELECT topic, payload, timestamp
FROM "/sys/+/+/thing/event/property/post"
```

Action：HTTP Webhook

- URL：`http://iot-access:8089/api/access/upstream`
- Method：`POST`
- Headers：`Content-Type: application/json`

Body 推荐写法（**payload 为 JSON 对象**时）：

```json
{
  "topic": "${topic}",
  "timestamp": ${timestamp},
  "payload": ${payload}
}
```

> 若将 `${payload}` 放进字符串（`"payload": "${payload}"`）且 payload 内含双引号，外层 JSON 会被破坏。
> 我们在 `iot-access` 侧做了“原始 body 容错解析”，但推荐在 EMQX 侧直接发送合法 JSON。

### 11.5 回调响应格式（非常重要）

EMQX 5.x HTTP Auth/Authz 回调要求 **`Content-Type: application/json`**，并在 body 中用 `result` 指示决策。

Auth（/auth）示例：

```json
{
  "result": "allow",
  "is_superuser": false
}
```

Authorization（/acl）示例：

```json
{
  "result": "allow"
}
```

若仍看到统计里 `No match` 增长，优先检查：

- 回调 body 是否是 JSON
- `result` 字段是否存在
- 是否返回了 200/204

### 11.6 客户端上下线 Webhook（入库连接记录）

用于在 EMQX 客户端连接/断开时调用 `iot-access`，复用管理面逻辑写入 `iot_device_connect_record` 并更新设备 `connect_status` / 最后上下线时间。

**`iot-access` 配置（`application.yml` 或环境变量）**：

- `iot.access.webhook.enabled`：`true` 时开放端点；`false`（默认）返回 404。
- `iot.access.webhook.secret`：`enabled=true` 时须配置非空；请求头 `X-Emqx-Webhook-Secret` 必须与本值完全一致，否则 401。

**EMQX 规则（示例，两条规则分别对应上线/下线）**：

- 数据源 SQL（上线）：

```sql
SELECT
  clientid,
  username,
  peername,
  node,
  timestamp,
  'client.connected' AS event
FROM "$events/client_connected"
```

- 数据源 SQL（下线）：

```sql
SELECT
  clientid,
  username,
  peername,
  reason,
  node,
  timestamp,
  'client.disconnected' AS event
FROM "$events/client_disconnected"
```

动作：**HTTP Server**（或数据集成 Webhook）

- URL：`http://iot-access:8089/api/access/emqx/webhook/session`
- Method：`POST`
- Headers：`Content-Type: application/json`，以及 `X-Emqx-Webhook-Secret: <与 iot.access.webhook.secret 相同>`

Body 使用「键值对」模板，将规则输出字段原样 JSON 序列化即可（与 11.4 类似，字段直接占位 `${clientid}` 等，勿把整段包成字符串）。

约定：**MQTT `username` 须为 `iot_device.device_key`**（与 HTTP 认证一致），否则无法匹配设备则跳过（响应仍 `200` + `{ "ok": false }`）。

---

## 12. 下行（方案1：通过 EMQX Management API publish）

### 12.1 下行链路

设备管理/应用使能 → 调用 `iot-access` 下行接口 → `iot-access` 调用 EMQX Management API → EMQX 将消息下发到设备订阅的 topic。

### 12.2 iot-access 下行接口

- `POST /api/access/downstream/publish`

请求体示例：

```json
{
  "traceId": "optional",
  "topic": "/sys/pk001/demo-001/thing/service/property/set",
  "payload": "{\"id\":\"1\",\"version\":\"1.0\",\"params\":{\"Temp\":25.0},\"method\":\"thing.service.property.set\"}",
  "qos": 1,
  "retain": false
}
```

返回：

- body：`ok`
- header：`X-Trace-Id: <traceId>`

### 12.3 EMQX 配置（API Key）

建议在 EMQX Dashboard 创建 API Key/Secret（用于调用 `/api/v5/publish`），并在 `iot-access` 中配置环境变量：

- `EMQX_API_KEY`
- `EMQX_API_SECRET`

默认 `base-url`（compose 网络内）：`http://emqx:18083`

> 注意：若 EMQX 开启了管理 API 认证但未配置 key/secret，publish 会返回 401/403。

---

## 13. 设备接入模块（iot-access）待实现清单

当前 `iot-access` 已实现：

- EMQX 5.x **Authentication**（HTTP，password_based）
- EMQX 5.x **Authorization/ACL**（HTTP）
- 上行落点：`POST /api/access/upstream`（MVP：结构化日志 + 容错解析）
- 下行 publish（方案1）：`POST /api/access/downstream/publish` → EMQX `/api/v5/publish`

下面是仍需补齐的能力（按优先级建议排序）。

### P0（下一迭代建议优先做）

- **上行标准化与投递**
  - 将上行统一转换为内部事件模型（如 `deviceId、messageType、timestamp、payload、traceId`）
  - 投递到 MQ（Kafka/RabbitMQ）或直接写入时序库（依你们整体架构选）
  - 幂等/去重：支持 `messageId/seq`，避免 webhook 重试导致重复入库
- **鉴权/ACL 数据源改造**
  - `/auth` 已支持从 `iot_device` 动态查库（可开关回滚）
  - `/acl` 已支持数据库策略模式（`iot_acl_policy`，默认关闭可灰度）
  - 后续增强：缓存失效、策略管理 API、细粒度约束（QoS/retain/payload）
- **可观测性**
  - 指标：Auth allow/deny、ACL allow/deny、Webhook 成功率、请求延迟、失败原因分布
  - Trace：为每条上行/下行生成 `traceId`，贯通到下游（已落地基础 traceId）

### P1（扩展与稳定性）

- **Topic/ACL 更细粒度**
  - 按阿里物模型路径细分：`thing/event`、`thing/service`、`thing/model` 的 publish/subscribe 白名单
  - 限制 retain、QoS、topic 长度、payload 大小等
- **下行工程化**
  - publish 失败重试/超时策略
  - 批量 publish（减少管理 API 调用次数）
  - 下行请求幂等（避免重复下发）
  - 回执关联（如 `.../set_reply` 与下发命令的关联 ID）

### P2（生产化与安全）

- **TLS/证书与生产部署基线**
  - EMQX `mqtts` 服务端证书配置（8883）
  - 是否需要 mTLS（设备证书）及证书生命周期（签发/烧录/吊销/轮换）
  - iot-access 内网通信是否启用 TLS
- **限流与防护**
  - 连接与消息限流策略（按设备/产品维度）
  - 风控：异常频率、恶意 topic 扫描、黑名单等
- **更完善的数据校验**
  - 针对 Alink JSON 的基本字段校验（id/method/params/time），错误码规范化



