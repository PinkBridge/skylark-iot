package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class UpsertThingModelRequest {
    @NotBlank(message = "modelJson cannot be empty")
    private String modelJson;

    @Size(max = 32, message = "version too long")
    private String version;
}

