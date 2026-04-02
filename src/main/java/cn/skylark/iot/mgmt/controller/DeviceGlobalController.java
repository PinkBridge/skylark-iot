package cn.skylark.iot.mgmt.controller;

import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.service.DeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DeviceGlobalController {

    private final DeviceService deviceService;

    public DeviceGlobalController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping("/api/mgmt/devices")
    public List<DeviceResponse> listAll() {
        return deviceService.listAll();
    }
}

