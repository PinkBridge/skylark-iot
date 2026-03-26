package cn.skylark.iot.access.listener;

import cn.skylark.iot.access.event.DeviceUpstreamEventPublished;
import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 落库 listener（占位实现）。
 * 后续可在这里对接 MySQL/TimescaleDB，把上行事件异步写入。
 */
@Component
public class UpstreamStorageListener {

    private static final Logger log = LoggerFactory.getLogger(UpstreamStorageListener.class);

    @Async("iotEventExecutor")
    @EventListener
    public void on(DeviceUpstreamEventPublished published) {
        if (published == null || published.getEvent() == null) {
            return;
        }
        DeviceUpstreamEvent e = published.getEvent();
        // TODO: persist
        log.debug("iot.upstream.storage.skip traceId={}, deviceId={}, messageType={}",
                e.getTraceId(), e.getDeviceId(), e.getMessageType());
    }
}

