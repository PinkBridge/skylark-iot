package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UpdateDeviceGroupRequest {
    @NotBlank(message = "name required")
    @Size(max = 128, message = "name too long")
    private String name;

    @Size(max = 512, message = "description too long")
    private String description;
}

