package cn.skylark.iot.access.listener;

import cn.skylark.iot.access.event.DeviceUpstreamEventPublished;
import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import cn.skylark.iot.access.service.ThingServiceReplyAwaiter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ThingServiceReplyListener {

    private final ThingServiceReplyAwaiter awaiter;

    public ThingServiceReplyListener(ThingServiceReplyAwaiter awaiter) {
        this.awaiter = awaiter;
    }

    @EventListener
    public void on(DeviceUpstreamEventPublished published) {
        if (published == null || published.getEvent() == null) {
            return;
        }
        DeviceUpstreamEvent e = published.getEvent();
        if (!"SERVICE_REPLY".equals(e.getEventType())) {
            return;
        }
        if (!StringUtils.hasText(e.getProductKey())
                || !StringUtils.hasText(e.getDeviceId())
                || !StringUtils.hasText(e.getServiceName())
                || !StringUtils.hasText(e.getMessageId())) {
            return;
        }
        String key = buildKey(e.getProductKey(), e.getDeviceId(), e.getServiceName(), e.getMessageId());
        awaiter.complete(key, e.getPayload());
    }

    public static String buildKey(String productKey, String deviceName, String identifier, String messageId) {
        return safe(productKey) + "|" + safe(deviceName) + "|" + safe(identifier) + "|" + safe(messageId);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

