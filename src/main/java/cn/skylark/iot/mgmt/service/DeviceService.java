package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceEventRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceRecordPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceServiceRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;

import java.util.List;

public interface DeviceService {

    DeviceResponse create(String productKey, CreateDeviceRequest req);

    DeviceResponse get(String productKey, String deviceKey);

    List<DeviceResponse> list(String productKey);

    List<DeviceResponse> listAll();

    DeviceResponse update(String productKey, String deviceKey, UpdateDeviceRequest req);

    DeviceResponse enable(String productKey, String deviceKey);

    DeviceResponse disable(String productKey, String deviceKey);

    DeviceResponse resetSecret(String productKey, String deviceKey);

    DevicePropertyRecordPageResponse listPropertyRecords(String productKey, String deviceKey, DeviceRecordPageQuery query);

    DeviceEventRecordPageResponse listEventRecords(String productKey, String deviceKey, DeviceRecordPageQuery query);

    DeviceServiceRecordPageResponse listServiceRecords(String productKey, String deviceKey, DeviceRecordPageQuery query);

    void delete(String productKey, String deviceKey);
}
