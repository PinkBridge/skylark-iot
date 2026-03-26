package cn.skylark.iot.access.service;

import cn.skylark.iot.access.model.UpstreamIngestRequest;

public interface UpstreamIngestService {

    void ingest(UpstreamIngestRequest request);
}
