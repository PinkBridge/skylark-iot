package cn.skylark.iot.mgmt.controller;

import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;
import cn.skylark.iot.mgmt.service.DeviceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/mgmt/products/{productKey}/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    public DeviceResponse create(@PathVariable("productKey") String productKey,
                                 @Validated @RequestBody CreateDeviceRequest request) {
        return deviceService.create(productKey, request);
    }

    @GetMapping
    public List<DeviceResponse> list(@PathVariable("productKey") String productKey) {
        return deviceService.list(productKey);
    }

    @GetMapping("/{deviceName}")
    public DeviceResponse get(@PathVariable("productKey") String productKey,
                              @PathVariable("deviceName") String deviceName) {
        return deviceService.get(productKey, deviceName);
    }

    @PutMapping("/{deviceName}")
    public DeviceResponse update(@PathVariable("productKey") String productKey,
                                 @PathVariable("deviceName") String deviceName,
                                 @Validated @RequestBody UpdateDeviceRequest request) {
        return deviceService.update(productKey, deviceName, request);
    }

    @PatchMapping("/{deviceName}/enable")
    public DeviceResponse enable(@PathVariable("productKey") String productKey,
                                 @PathVariable("deviceName") String deviceName) {
        return deviceService.enable(productKey, deviceName);
    }

    @PatchMapping("/{deviceName}/disable")
    public DeviceResponse disable(@PathVariable("productKey") String productKey,
                                  @PathVariable("deviceName") String deviceName) {
        return deviceService.disable(productKey, deviceName);
    }

    @PostMapping("/{deviceName}/reset-secret")
    public DeviceResponse resetSecret(@PathVariable("productKey") String productKey,
                                      @PathVariable("deviceName") String deviceName) {
        return deviceService.resetSecret(productKey, deviceName);
    }
}

