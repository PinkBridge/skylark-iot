package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UpdateDeviceRequest {
    @NotBlank(message = "deviceName required")
    @Size(max = 128, message = "deviceName too long")
    private String deviceName;
}
