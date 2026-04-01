package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;

import java.util.List;

public interface DeviceService {

    DeviceResponse create(String productKey, CreateDeviceRequest req);

    DeviceResponse get(String productKey, String deviceName);

    List<DeviceResponse> list(String productKey);

    DeviceResponse update(String productKey, String deviceName, UpdateDeviceRequest req);

    DeviceResponse enable(String productKey, String deviceName);

    DeviceResponse disable(String productKey, String deviceName);

    DeviceResponse resetSecret(String productKey, String deviceName);

    void delete(String productKey, String deviceName);
}
