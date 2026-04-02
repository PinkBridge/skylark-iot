package cn.skylark.iot.mgmt.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductEntity {
    private Long id;
    private Long tenantId;
    private String productKey;
    /** 后台创建/复制时自动生成，16 位 */
    private String productSecret;
    private String name;
    private String coverImageUrl;
    private String thumbnailUrl;
    private String description;
    private String protocolType;
    private String deviceType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

