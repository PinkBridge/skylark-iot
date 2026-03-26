package cn.skylark.iot.access.service;

import cn.skylark.iot.access.model.DownstreamPublishRequest;

import java.util.Optional;

public interface EmqxManagementClient {

    Optional<String> publish(DownstreamPublishRequest req);
}
