package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DeviceImportResult {
    private int successCount;
    private int failCount;
    private List<DeviceImportErrorItem> errors = new ArrayList<>();
}
