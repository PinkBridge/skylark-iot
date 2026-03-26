package cn.skylark.iot.access.listener;

import cn.skylark.iot.access.event.DeviceUpstreamEventPublished;
import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class UpstreamLoggingListener {

    private static final Logger log = LoggerFactory.getLogger(UpstreamLoggingListener.class);

    @Async("iotEventExecutor")
    @EventListener
    public void on(DeviceUpstreamEventPublished published) {
        if (published == null || published.getEvent() == null) {
            return;
        }
        DeviceUpstreamEvent e = published.getEvent();
        log.info("iot.upstream.event traceId={}, deviceId={}, messageType={}, ts={}, topic={}, payload={}",
                e.getTraceId(),
                e.getDeviceId(),
                e.getMessageType(),
                e.getTimestamp(),
                e.getTopic(),
                abbreviate(e.getPayload(), 500));
    }

    private static String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}

