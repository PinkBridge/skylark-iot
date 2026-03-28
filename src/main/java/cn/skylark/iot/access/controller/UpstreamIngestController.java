package cn.skylark.iot.access.controller;

import cn.skylark.iot.access.model.UpstreamIngestRequest;
import cn.skylark.iot.access.service.UpstreamIngestService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

@RestController
@RequestMapping("/api/access")
public class UpstreamIngestController {

    private static final Logger log = LoggerFactory.getLogger(UpstreamIngestController.class);

    private final UpstreamIngestService ingestService;
    private final ObjectMapper objectMapper;

    public UpstreamIngestController(UpstreamIngestService ingestService, ObjectMapper objectMapper) {
        this.ingestService = ingestService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/upstream")
    public ResponseEntity<String> ingest(@RequestBody(required = false) String rawBody) {
        log.info("iot.upstream.raw body={}", abbreviate(rawBody, 4000));
        UpstreamIngestRequest request = parseToRequest(rawBody);
        ingestService.ingest(request);
        return ResponseEntity.ok()
                .header("X-Trace-Id", request.getTraceId() == null ? "" : request.getTraceId())
                .body("ok");
    }

    private UpstreamIngestRequest parseToRequest(String rawBody) {
        UpstreamIngestRequest r = new UpstreamIngestRequest();
        if (rawBody == null) {
            return r;
        }
        String s = rawBody.trim();
        if (s.isEmpty()) {
            return r;
        }
        // 兼容 EMQX body 模板错误导致的非 JSON：先原样塞 payload，确保不会 400
        r.setPayload(s);
        r.setTraceId(newTraceId());
        if (!s.startsWith("{") || !s.endsWith("}")) {
            return r;
        }
        try {
            JsonNode root = objectMapper.readTree(s);
            if (root.has("traceId")) {
                String tid = root.path("traceId").asText(null);
                if (tid != null && !tid.trim().isEmpty()) {
                    r.setTraceId(tid.trim());
                }
            }
            // EMQX Rules/Webhook 常用字段名：topic / mqtt_topic
            if (root.has("topic")) {
                r.setTopic(root.path("topic").asText(null));
            } else if (root.has("mqtt_topic")) {
                r.setTopic(root.path("mqtt_topic").asText(null));
            }
            if (root.has("timestamp") && root.path("timestamp").canConvertToLong()) {
                r.setTimestamp(root.path("timestamp").asLong());
            }
            if (root.has("deviceId")) {
                r.setDeviceId(root.path("deviceId").asText(null));
            }
            if (root.has("messageType")) {
                r.setMessageType(root.path("messageType").asText(null));
            }
            if (root.has("payload")) {
                JsonNode p = root.path("payload");
                // 兼容两种写法：
                // 1) "payload": "${payload}"（字符串，可能包含 JSON 文本）
                // 2) "payload": ${payload}（对象/数组，合法 JSON）
                r.setPayload(p.isTextual() ? p.asText() : p.toString());
            }
        } catch (Exception ignore) {
            // EMQX 常见错误配置：把 ${payload} 直接放进 JSON 字符串，导致引号未转义，整体不是合法 JSON。
            // 这里做降级提取：尽量从原始文本中抠出 topic/timestamp/payload，保证后续链路可用。
            fillFromBrokenJsonLikeBody(r, s);
        }
        return r;
    }

    private static final Pattern TOPIC_PATTERN = Pattern.compile("\"topic\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\"timestamp\"\\s*:\\s*\"?(\\d{10,})\"?");
    private static final Pattern PAYLOAD_PATTERN = Pattern.compile("\"payload\"\\s*:\\s*\"(.*)\"\\s*,\\s*\"timestamp\"", Pattern.DOTALL);

    private void fillFromBrokenJsonLikeBody(UpstreamIngestRequest r, String s) {
        if (r.getTraceId() == null || r.getTraceId().trim().isEmpty()) {
            r.setTraceId(newTraceId());
        }
        if (r.getTopic() == null) {
            Matcher m = TOPIC_PATTERN.matcher(s);
            if (m.find()) {
                r.setTopic(m.group(1));
            }
        }
        if (r.getTimestamp() == null) {
            Matcher m = TIMESTAMP_PATTERN.matcher(s);
            if (m.find()) {
                try {
                    r.setTimestamp(Long.parseLong(m.group(1)));
                } catch (NumberFormatException ignore) {
                    // ignore
                }
            }
        }
        // payload：优先抽出 payload 字段的字符串内容（即便里面含未转义 JSON）
        Matcher pm = PAYLOAD_PATTERN.matcher(s);
        if (pm.find()) {
            String payload = pm.group(1);
            if (payload != null) {
                r.setPayload(payload.trim());
            }
        }
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
