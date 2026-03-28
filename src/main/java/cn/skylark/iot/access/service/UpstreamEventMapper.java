package cn.skylark.iot.access.service;

import cn.skylark.iot.access.mapper.AccessDeviceMapper;
import cn.skylark.iot.access.model.AccessDeviceRecord;
import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import cn.skylark.iot.access.model.UpstreamIngestRequest;
import cn.skylark.iot.access.protocol.ParseResult;
import cn.skylark.iot.access.protocol.ProtocolHandleResult;
import cn.skylark.iot.access.protocol.ProtocolContext;
import cn.skylark.iot.access.protocol.ProtocolResolver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class UpstreamEventMapper {

    private final AccessDeviceMapper accessDeviceMapper;
    private final ProtocolResolver protocolResolver;

    public UpstreamEventMapper(AccessDeviceMapper accessDeviceMapper, ProtocolResolver protocolResolver) {
        this.accessDeviceMapper = accessDeviceMapper;
        this.protocolResolver = protocolResolver;
    }

    public DeviceUpstreamEvent toEvent(UpstreamIngestRequest request) {
        DeviceUpstreamEvent e = new DeviceUpstreamEvent();
        if (request == null) {
            e.setTimestamp(System.currentTimeMillis());
            e.setPayloadValid(false);
            e.setParseError("request is null");
            e.setEventType("INVALID_REQUEST");
            e.setMessageType("unknown");
            return e;
        }

        e.setTraceId(trimToNull(request.getTraceId()));
        e.setDeviceId(trimToNull(request.getDeviceId()));
        e.setMessageType(trimToNull(request.getMessageType()));
        e.setTopic(trimToNull(request.getTopic()));
        e.setPayload(request.getPayload());

        Long ts = request.getTimestamp();
        e.setTimestamp(ts == null ? System.currentTimeMillis() : ts);

        if (e.getTopic() != null && (!isText(e.getDeviceId()) || !isText(e.getMessageType()))) {
            Derived derived = deriveFromAliTopic(e.getDeviceId(), e.getMessageType(), e.getTopic());
            if (!isText(e.getDeviceId())) {
                e.setDeviceId(derived.deviceId);
            }
            if (!isText(e.getMessageType())) {
                e.setMessageType(derived.messageType);
            }
        }

        AccessDeviceRecord device = findEnabledDevice(e.getDeviceId());
        if (device != null) {
            e.setProductKey(trimToNull(device.getProductKey()));
            e.setProtocolType(trimToNull(device.getProtocolType()));
        }

        ProtocolContext context = new ProtocolContext();
        context.setTraceId(e.getTraceId());
        context.setTopic(e.getTopic());
        context.setPayload(e.getPayload());
        context.setTimestamp(e.getTimestamp());
        context.setDeviceId(e.getDeviceId());
        context.setProductKey(e.getProductKey());
        context.setProtocolType(e.getProtocolType());
        ProtocolHandleResult handleResult = protocolResolver.resolveAndHandle(context);
        ParseResult result = handleResult.getParseResult();
        e.setProtocolType(trimToNull(result.getProtocolType()));
        e.setEventType(trimToNull(result.getEventType()));
        if (isText(result.getMessageType())) {
            e.setMessageType(trimToNull(result.getMessageType()));
        }
        e.setMessageId(trimToNull(result.getMessageId()));
        e.setEventName(trimToNull(result.getEventName()));
        e.setServiceName(trimToNull(result.getServiceName()));
        e.setAlinkMethod(trimToNull(result.getAlinkMethod()));
        e.setPayloadValid(result.getPayloadValid());
        e.setParseError(trimToNull(result.getParseError()));

        if (!isText(e.getMessageType())) {
            e.setMessageType("unknown");
        }

        return e;
    }

    private static boolean isText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private AccessDeviceRecord findEnabledDevice(String deviceName) {
        if (!isText(deviceName)) {
            return null;
        }
        List<AccessDeviceRecord> devices = accessDeviceMapper.findByDeviceName(deviceName.trim());
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        for (AccessDeviceRecord item : devices) {
            if (item == null) {
                continue;
            }
            if (!deviceName.trim().equals(item.getDeviceName())) {
                continue;
            }
            if ("enabled".equalsIgnoreCase(trimToNull(item.getStatus()))) {
                return item;
            }
        }
        return null;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static class Derived {
        final String deviceId;
        final String messageType;

        private Derived(String deviceId, String messageType) {
            this.deviceId = deviceId;
            this.messageType = messageType;
        }
    }

    /**
     * 从阿里物模型 Topic 解析 deviceId 与 messageType（轻量 MVP 版）。
     *
     * topic 形态（示例）：
     * - /sys/{productKey}/{deviceName}/thing/event/property/post
     * - /sys/{productKey}/{deviceName}/thing/model/up_raw
     * - /sys/{productKey}/{deviceName}/thing/event/{eventId}/post
     */
    private static Derived deriveFromAliTopic(String currentDeviceId, String currentMessageType, String topic) {
        String deviceId = currentDeviceId;
        String messageType = currentMessageType;

        if (!isText(topic)) {
            return new Derived(deviceId, messageType);
        }

        String[] parts = topic.split("/");
        if (!isText(deviceId) && parts.length >= 4) {
            deviceId = parts[3];
        }

        if (!isText(messageType)) {
            String t = topic.trim();
            String lower = t.toLowerCase(Locale.ROOT);
            if (lower.contains("thing/event/property/post")) {
                messageType = "properties";
            } else if (lower.contains("thing/model/up_raw")) {
                messageType = "raw";
            } else if (lower.contains("/thing/event/") && lower.endsWith("/post")) {
                int idx = lower.indexOf("/thing/event/");
                int end = lower.lastIndexOf("/post");
                if (idx >= 0 && end > idx + "/thing/event/".length()) {
                    // 用原字符串切，保留 eventId 大小写
                    String eventId = t.substring(idx + "/thing/event/".length(), end);
                    messageType = "event:" + eventId;
                } else {
                    messageType = "event";
                }
            } else {
                messageType = "unknown";
            }
        }

        return new Derived(deviceId, messageType);
    }
}

