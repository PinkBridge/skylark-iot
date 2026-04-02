package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeviceEventRecordPageResponse {
    private List<DeviceEventRecordResponse> records;
    private long total;
    private int pageNum;
    private int pageSize;
}
