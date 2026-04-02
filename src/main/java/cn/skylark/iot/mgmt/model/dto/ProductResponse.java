package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class ProductResponse {
    private String productKey;
    private String name;
    private String coverImageUrl;
    private String thumbnailUrl;
    private String description;
    private String protocolType;
    private String deviceType;
    private String status;
    private Long deviceCount;
}

