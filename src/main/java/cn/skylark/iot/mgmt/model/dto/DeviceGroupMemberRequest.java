package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class DeviceGroupMemberRequest {
    @NotBlank(message = "productKey required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "productKey format invalid")
    private String productKey;

    @NotBlank(message = "deviceKey required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "deviceKey format invalid")
    private String deviceKey;
}

