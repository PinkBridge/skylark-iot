package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class ProductResponse {
    private String productKey;
    private String name;
    private String description;
    private String status;
}

