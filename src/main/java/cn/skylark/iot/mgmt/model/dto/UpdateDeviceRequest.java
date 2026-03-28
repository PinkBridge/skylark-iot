package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class UpdateDeviceRequest {
    @Size(max = 128, message = "displayName too long")
    private String displayName;

    @Pattern(regexp = "^[A-Z0-9_]{2,32}$", message = "protocolType format invalid")
    private String protocolType;

    @Size(max = 32, message = "protocolVersion too long")
    private String protocolVersion;
}

