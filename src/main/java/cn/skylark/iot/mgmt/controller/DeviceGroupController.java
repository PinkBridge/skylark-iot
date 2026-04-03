package cn.skylark.iot.mgmt.controller;

import cn.skylark.iot.mgmt.model.dto.AddDevicesToGroupRequest;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceGroupRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceGroupRequest;
import cn.skylark.iot.mgmt.service.DeviceGroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mgmt/device-groups")
public class DeviceGroupController {
    private final DeviceGroupService deviceGroupService;

    public DeviceGroupController(DeviceGroupService deviceGroupService) {
        this.deviceGroupService = deviceGroupService;
    }

    @GetMapping
    public DeviceGroupPageResponse list(@ModelAttribute DeviceGroupPageQuery query) {
        return deviceGroupService.list(query);
    }

    @PostMapping
    public DeviceGroupResponse create(@Validated @RequestBody CreateDeviceGroupRequest request) {
        return deviceGroupService.create(request);
    }

    @GetMapping("/{groupKey}")
    public DeviceGroupResponse get(@PathVariable("groupKey") String groupKey) {
        return deviceGroupService.get(groupKey);
    }

    @PutMapping("/{groupKey}")
    public DeviceGroupResponse update(@PathVariable("groupKey") String groupKey,
                                      @Validated @RequestBody UpdateDeviceGroupRequest request) {
        return deviceGroupService.update(groupKey, request);
    }

    @DeleteMapping("/{groupKey}")
    public void delete(@PathVariable("groupKey") String groupKey) {
        deviceGroupService.delete(groupKey);
    }

    @GetMapping("/{groupKey}/devices")
    public List<DeviceResponse> listGroupDevices(@PathVariable("groupKey") String groupKey) {
        return deviceGroupService.listGroupDevices(groupKey);
    }

    @PostMapping("/{groupKey}/devices")
    public void addDevices(@PathVariable("groupKey") String groupKey,
                           @Validated @RequestBody AddDevicesToGroupRequest request) {
        deviceGroupService.addDevices(groupKey, request);
    }

    @DeleteMapping("/{groupKey}/devices/{productKey}/{deviceKey}")
    public void removeDevice(@PathVariable("groupKey") String groupKey,
                             @PathVariable("productKey") String productKey,
                             @PathVariable("deviceKey") String deviceKey) {
        deviceGroupService.removeDevice(groupKey, productKey, deviceKey);
    }
}

