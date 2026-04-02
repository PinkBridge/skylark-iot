package cn.skylark.iot.mgmt.controller;

import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceEventRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceRecordPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceServiceRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;
import cn.skylark.iot.mgmt.service.DeviceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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

    @GetMapping("/{deviceKey}")
    public DeviceResponse get(@PathVariable("productKey") String productKey,
                              @PathVariable("deviceKey") String deviceKey) {
        return deviceService.get(productKey, deviceKey);
    }

    @PutMapping("/{deviceKey}")
    public DeviceResponse update(@PathVariable("productKey") String productKey,
                                 @PathVariable("deviceKey") String deviceKey,
                                 @Validated @RequestBody UpdateDeviceRequest request) {
        return deviceService.update(productKey, deviceKey, request);
    }

    @PatchMapping("/{deviceKey}/enable")
    public DeviceResponse enable(@PathVariable("productKey") String productKey,
                                 @PathVariable("deviceKey") String deviceKey) {
        return deviceService.enable(productKey, deviceKey);
    }

    @PatchMapping("/{deviceKey}/disable")
    public DeviceResponse disable(@PathVariable("productKey") String productKey,
                                  @PathVariable("deviceKey") String deviceKey) {
        return deviceService.disable(productKey, deviceKey);
    }

    @PostMapping("/{deviceKey}/reset-secret")
    public DeviceResponse resetSecret(@PathVariable("productKey") String productKey,
                                      @PathVariable("deviceKey") String deviceKey) {
        return deviceService.resetSecret(productKey, deviceKey);
    }

    @GetMapping("/{deviceKey}/property-records")
    public DevicePropertyRecordPageResponse listPropertyRecords(@PathVariable("productKey") String productKey,
                                                                @PathVariable("deviceKey") String deviceKey,
                                                                @ModelAttribute DeviceRecordPageQuery query) {
        return deviceService.listPropertyRecords(productKey, deviceKey, query);
    }

    @GetMapping("/{deviceKey}/event-records")
    public DeviceEventRecordPageResponse listEventRecords(@PathVariable("productKey") String productKey,
                                                          @PathVariable("deviceKey") String deviceKey,
                                                          @ModelAttribute DeviceRecordPageQuery query) {
        return deviceService.listEventRecords(productKey, deviceKey, query);
    }

    @GetMapping("/{deviceKey}/service-records")
    public DeviceServiceRecordPageResponse listServiceRecords(@PathVariable("productKey") String productKey,
                                                              @PathVariable("deviceKey") String deviceKey,
                                                              @ModelAttribute DeviceRecordPageQuery query) {
        return deviceService.listServiceRecords(productKey, deviceKey, query);
    }

    @DeleteMapping("/{deviceKey}")
    public void delete(@PathVariable("productKey") String productKey,
                       @PathVariable("deviceKey") String deviceKey) {
        deviceService.delete(productKey, deviceKey);
    }
}
