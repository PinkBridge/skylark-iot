package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.AddDevicesToGroupRequest;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceGroupRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceGroupRequest;

import java.util.List;

public interface DeviceGroupService {
    DeviceGroupResponse create(CreateDeviceGroupRequest request);

    DeviceGroupResponse get(String groupKey);

    DeviceGroupPageResponse list(DeviceGroupPageQuery query);

    DeviceGroupResponse update(String groupKey, UpdateDeviceGroupRequest request);

    void delete(String groupKey);

    void addDevices(String groupKey, AddDevicesToGroupRequest request);

    void removeDevice(String groupKey, String productKey, String deviceKey);

    List<DeviceResponse> listGroupDevices(String groupKey);
}

