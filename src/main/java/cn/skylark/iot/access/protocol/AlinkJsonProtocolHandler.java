package cn.skylark.iot.access.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cn.skylark.iot.access.model.DownstreamPublishRequest;
import cn.skylark.iot.access.service.EmqxManagementClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(10)
public class AlinkJsonProtocolHandler implements ProtocolHandler {

    private static final Logger log = LoggerFactory.getLogger(AlinkJsonProtocolHandler.class);

    private static final Pattern EVENT_TOPIC = Pattern.compile("/thing/event/([^/]+)/post$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SERVICE_REPLY_TOPIC = Pattern.compile("/thing/service/([^/]+)/reply$", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final EmqxManagementClient emqxManagementClient;

    public AlinkJsonProtocolHandler(ObjectMapper objectMapper, EmqxManagementClient emqxManagementClient) {
        this.objectMapper = objectMapper;
        this.emqxManagementClient = emqxManagementClient;
    }

    @Override
    public boolean supports(ProtocolContext ctx) {
        if (ctx == null) {
            return false;
        }
        String protocolType = safe(ctx.getProtocolType()).toUpperCase(Locale.ROOT);
        if ("MQTT_ALINK_JSON".equals(protocolType) || "ALINK_JSON".equals(protocolType)) {
            return true;
        }
        String topic = safe(ctx.getTopic()).toLowerCase(Locale.ROOT);
        return topic.startsWith("/sys/");
    }

    @Override
    public ParseResult parse(ProtocolContext ctx) {
        ParseResult result = new ParseResult();
        result.setProtocolType("MQTT_ALINK_JSON");
        classifyByTopic(result, safe(ctx.getTopic()));
        String payload = safe(ctx.getPayload());
        if (!StringUtils.hasText(payload)) {
            result.setParseStatus(ParseResult.ParseStatus.INVALID);
            result.setParseError("payload is empty");
            result.setPayloadValid(false);
            return result;
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            result.setPayloadValid(true);
            result.setParseStatus(ParseResult.ParseStatus.OK);
            if (root.has("id")) {
                result.setMessageId(root.path("id").asText(null));
            }
            if (root.has("method")) {
                String method = root.path("method").asText(null);
                result.setAlinkMethod(method);
                classifyByMethod(result, safe(method));
            }
            if (root.has("code") && root.has("data") && !StringUtils.hasText(result.getEventType())) {
                result.setEventType("SERVICE_REPLY");
                if (!StringUtils.hasText(result.getMessageType())) {
                    result.setMessageType("service_reply");
                }
            }
            if (!StringUtils.hasText(result.getMessageType())) {
                result.setMessageType("unknown");
            }
            return result;
        } catch (Exception ex) {
            result.setParseStatus(ParseResult.ParseStatus.INVALID);
            result.setParseError("payload parse failed: " + ex.getMessage());
            result.setPayloadValid(false);
            if (!StringUtils.hasText(result.getMessageType())) {
                result.setMessageType("unknown");
            }
            return result;
        }
    }

    @Override
    public void ack(ParseResult result, ProtocolContext ctx) {
        if (result == null || ctx == null || !StringUtils.hasText(ctx.getTopic())) {
            return;
        }
        String ackTopic = buildAckTopic(result, ctx.getTopic());
        if (!StringUtils.hasText(ackTopic)) {
            return;
        }
        log.info("iot.upstream.ack.prepare traceId={}, eventType={}, topic={}, ackTopic={}",
                ctx.getTraceId(), result.getEventType(), ctx.getTopic(), ackTopic);
        boolean success = ParseResult.ParseStatus.OK.equals(result.getParseStatus());
        String id = StringUtils.hasText(result.getMessageId()) ? result.getMessageId().trim() : "0";
        String ackMethod = buildAckMethod(result);
        String message = success ? "success" : safe(result.getParseError());
        if (!StringUtils.hasText(message)) {
            message = success ? "success" : "invalid payload";
        }

        ObjectNode root = objectMapper.createObjectNode();
        root.put("id", id);
        root.put("code", success ? 200 : 500);
        root.put("message", message);
        if (StringUtils.hasText(ackMethod)) {
            root.put("method", ackMethod);
        }
        root.set("data", objectMapper.createObjectNode());

        DownstreamPublishRequest req = new DownstreamPublishRequest();
        req.setTraceId(ctx.getTraceId());
        req.setTopic(ackTopic);
        req.setPayload(root.toString());
        req.setQos(1);
        req.setRetain(false);
        emqxManagementClient.publish(req);
    }

    private static String buildAckTopic(ParseResult result, String originTopic) {
        String topic = safe(originTopic);
        String lower = topic.toLowerCase(Locale.ROOT);
        if ("PROPERTY_POST".equals(result.getEventType()) && lower.endsWith("/thing/event/property/post")) {
            return topic.substring(0, topic.length() - "/thing/event/property/post".length()) + "/thing/event/property/post_reply";
        }
        if ("EVENT_POST".equals(result.getEventType()) && StringUtils.hasText(result.getEventName())) {
            String suffix = "/thing/event/" + result.getEventName() + "/post";
            if (lower.endsWith(suffix.toLowerCase(Locale.ROOT))) {
                return topic.substring(0, topic.length() - suffix.length()) + "/thing/event/" + result.getEventName() + "/post_reply";
            }
        }
        if ("SERVICE_REPLY".equals(result.getEventType()) && StringUtils.hasText(result.getServiceName())) {
            String suffix = "/thing/service/" + result.getServiceName() + "/reply";
            if (lower.endsWith(suffix.toLowerCase(Locale.ROOT))) {
                return topic.substring(0, topic.length() - suffix.length()) + "/thing/service/" + result.getServiceName() + "/reply_ack";
            }
        }
        return null;
    }

    private static String buildAckMethod(ParseResult result) {
        if ("PROPERTY_POST".equals(result.getEventType())) {
            return "thing.event.property.post_reply";
        }
        if ("EVENT_POST".equals(result.getEventType()) && StringUtils.hasText(result.getEventName())) {
            return "thing.event." + result.getEventName() + ".post_reply";
        }
        if ("SERVICE_REPLY".equals(result.getEventType()) && StringUtils.hasText(result.getServiceName())) {
            return "thing.service." + result.getServiceName() + ".reply_ack";
        }
        return null;
    }

    private static void classifyByTopic(ParseResult result, String topic) {
        String original = safe(topic);
        String t = original.toLowerCase(Locale.ROOT);
        if (t.contains("/thing/event/property/post")) {
            result.setEventType("PROPERTY_POST");
            result.setMessageType("properties");
            return;
        }
        // 在原始 topic 上匹配：CASE_INSENSITIVE 只用于路径字面量，捕获段保留设备侧大小写（如 Alarm）
        Matcher em = EVENT_TOPIC.matcher(original);
        if (em.find()) {
            String eventName = em.group(1);
            result.setEventType("EVENT_POST");
            result.setEventName(eventName);
            result.setMessageType("event:" + eventName);
            return;
        }
        Matcher sm = SERVICE_REPLY_TOPIC.matcher(original);
        if (sm.find()) {
            String serviceName = sm.group(1);
            result.setEventType("SERVICE_REPLY");
            result.setServiceName(serviceName);
            result.setMessageType("service_reply");
        }
    }

    private static void classifyByMethod(ParseResult result, String method) {
        String raw = safe(method);
        String m = raw.toLowerCase(Locale.ROOT);
        if (m.equals("thing.event.property.post")) {
            result.setEventType("PROPERTY_POST");
            result.setMessageType("properties");
            return;
        }
        if (m.startsWith("thing.event.") && m.endsWith(".post")) {
            String eventName = raw.substring("thing.event.".length(), raw.length() - ".post".length());
            result.setEventType("EVENT_POST");
            result.setEventName(eventName);
            result.setMessageType("event:" + eventName);
            return;
        }
        if (m.startsWith("thing.service.") && m.endsWith(".reply")) {
            String serviceName = raw.substring("thing.service.".length(), raw.length() - ".reply".length());
            result.setEventType("SERVICE_REPLY");
            result.setServiceName(serviceName);
            result.setMessageType("service_reply");
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

