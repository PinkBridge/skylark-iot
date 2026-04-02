package cn.skylark.iot.mgmt.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
public class ProductResponse {
    private String productKey;
    /** 仅详情/创建/复制等单条接口返回；列表不返回 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String productSecret;
    private String name;
    private String coverImageUrl;
    private String thumbnailUrl;
    private String description;
    private String protocolType;
    private String deviceType;
    private String status;
    private Long deviceCount;
}

