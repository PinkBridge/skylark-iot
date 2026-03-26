package cn.skylark.iot.access.listener;

import cn.skylark.iot.access.event.DeviceUpstreamEventPublished;
import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 队列投递 listener（占位实现）。
 * 后续可在这里对接 RabbitMQ/Kafka，把 DeviceUpstreamEvent 序列化后入队。
 */
@Component
public class UpstreamQueueListener {

    private static final Logger log = LoggerFactory.getLogger(UpstreamQueueListener.class);

    @Async("iotEventExecutor")
    @EventListener
    public void on(DeviceUpstreamEventPublished published) {
        if (published == null || published.getEvent() == null) {
            return;
        }
        DeviceUpstreamEvent e = published.getEvent();
        // TODO: enqueue
        log.debug("iot.upstream.queue.skip traceId={}, deviceId={}, messageType={}",
                e.getTraceId(), e.getDeviceId(), e.getMessageType());
    }
}

