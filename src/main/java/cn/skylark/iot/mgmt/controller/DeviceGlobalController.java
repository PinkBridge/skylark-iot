package cn.skylark.iot.mgmt.controller;

import cn.skylark.iot.mgmt.model.dto.DeviceImportResult;
import cn.skylark.iot.mgmt.model.dto.DevicePageQuery;
import cn.skylark.iot.mgmt.model.dto.DevicePageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.service.DeviceExcelImportService;
import cn.skylark.iot.mgmt.service.DeviceService;
import cn.skylark.iot.mgmt.service.MgmtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
public class DeviceGlobalController {

    private final DeviceService deviceService;
    private final DeviceExcelImportService deviceExcelImportService;

    public DeviceGlobalController(DeviceService deviceService,
                                  DeviceExcelImportService deviceExcelImportService) {
        this.deviceService = deviceService;
        this.deviceExcelImportService = deviceExcelImportService;
    }

    @GetMapping("/api/mgmt/devices")
    public List<DeviceResponse> listAll() {
        return deviceService.listAll();
    }

    @GetMapping("/api/mgmt/devices/page")
    public DevicePageResponse listPage(@ModelAttribute DevicePageQuery query) {
        return deviceService.listAllPage(query);
    }

    @GetMapping("/api/mgmt/devices/import-template")
    public ResponseEntity<byte[]> downloadDeviceImportTemplate() {
        try {
            byte[] bytes = deviceExcelImportService.buildTemplateXlsx();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"device-import-template.xlsx\"");
            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (IOException e) {
            throw new MgmtException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to build template");
        }
    }

    @PostMapping(value = "/api/mgmt/devices/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DeviceImportResult importDevices(@RequestParam("file") MultipartFile file) {
        return deviceExcelImportService.importFromExcel(file);
    }
}

