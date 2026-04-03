package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class DevicePageQuery {
    /** 模糊匹配 device_name / device_key / product_key */
    private String keyword;
    private Integer pageNum = 1;
    private Integer pageSize = 20;
}
