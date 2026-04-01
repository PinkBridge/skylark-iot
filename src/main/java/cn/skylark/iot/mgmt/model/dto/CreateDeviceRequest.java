package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CreateDeviceRequest {
    @NotBlank(message = "deviceName cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "deviceName format invalid")
    private String deviceName;

    @Size(max = 128, message = "displayName too long")
    private String displayName;

    @Size(max = 256, message = "secret too long")
    private String secret;

    @Size(max = 32, message = "protocolVersion too long")
    private String protocolVersion;
}

