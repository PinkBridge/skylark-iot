package cn.skylark.iot.access.service;

import cn.skylark.iot.access.event.DeviceUpstreamEventPublished;
import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class SpringDeviceEventBus implements DeviceEventBus {

    private final ApplicationEventPublisher publisher;

    public SpringDeviceEventBus(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(DeviceUpstreamEvent event) {
        publisher.publishEvent(new DeviceUpstreamEventPublished(event));
    }
}

