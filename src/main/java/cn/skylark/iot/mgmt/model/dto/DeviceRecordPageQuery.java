package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class DeviceRecordPageQuery {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
