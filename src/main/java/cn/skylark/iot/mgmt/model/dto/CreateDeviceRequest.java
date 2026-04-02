package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateDeviceRequest {
    @NotBlank(message = "deviceName cannot be empty")
    @Size(min = 1, max = 128, message = "deviceName length invalid")
    private String deviceName;
}
