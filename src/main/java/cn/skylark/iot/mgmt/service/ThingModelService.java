package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.ThingModelResponse;
import cn.skylark.iot.mgmt.model.dto.UpsertThingModelRequest;

public interface ThingModelService {

    ThingModelResponse upsert(String productKey, UpsertThingModelRequest request);

    ThingModelResponse get(String productKey);
}
