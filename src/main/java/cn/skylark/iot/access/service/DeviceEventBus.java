package cn.skylark.iot.access.service;

import cn.skylark.iot.access.model.DeviceUpstreamEvent;

public interface DeviceEventBus {

    void publish(DeviceUpstreamEvent event);
}

