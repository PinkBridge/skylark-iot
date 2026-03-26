package cn.skylark.iot.access.event;

import cn.skylark.iot.access.model.DeviceUpstreamEvent;

/**
 * Spring 事件包装：用于解耦“发布”与“异步处理”。
 */
public class DeviceUpstreamEventPublished {

    private final DeviceUpstreamEvent event;

    public DeviceUpstreamEventPublished(DeviceUpstreamEvent event) {
        this.event = event;
    }

    public DeviceUpstreamEvent getEvent() {
        return event;
    }
}

