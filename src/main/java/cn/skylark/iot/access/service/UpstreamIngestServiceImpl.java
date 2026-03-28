package cn.skylark.iot.access.service;

import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import cn.skylark.iot.access.model.UpstreamIngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UpstreamIngestServiceImpl implements UpstreamIngestService {

    private static final Logger log = LoggerFactory.getLogger(UpstreamIngestServiceImpl.class);

    private final UpstreamEventMapper mapper;
    private final DeviceEventBus eventBus;

    public UpstreamIngestServiceImpl(UpstreamEventMapper mapper,
                                     DeviceEventBus eventBus) {
        this.mapper = mapper;
        this.eventBus = eventBus;
    }

    @Override
    public void ingest(UpstreamIngestRequest request) {
        DeviceUpstreamEvent event = mapper.toEvent(request);
        eventBus.publish(event);
        log.debug("iot.upstream.published traceId={}, deviceId={}, messageType={}, ts={}, topic={}",
                event.getTraceId(),
                event.getDeviceId(),
                event.getMessageType(),
                event.getTimestamp(),
                event.getTopic());
    }

    // 不在这里做 payload 打印：交给 listener，避免同步链路放大开销
}

