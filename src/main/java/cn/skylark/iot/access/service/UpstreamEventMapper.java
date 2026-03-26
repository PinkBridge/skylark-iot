package cn.skylark.iot.access.service;

import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import cn.skylark.iot.access.model.UpstreamIngestRequest;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class UpstreamEventMapper {

    public DeviceUpstreamEvent toEvent(UpstreamIngestRequest request) {
        DeviceUpstreamEvent e = new DeviceUpstreamEvent();
        if (request == null) {
            e.setTimestamp(System.currentTimeMillis());
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

        return e;
    }

    private static boolean isText(String s) {
        return s != null && !s.trim().isEmpty();
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

