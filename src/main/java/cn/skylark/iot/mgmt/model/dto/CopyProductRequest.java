package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CopyProductRequest {
    @NotBlank(message = "targetProductKey cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "targetProductKey format invalid")
    private String targetProductKey;

    @NotBlank(message = "targetName cannot be empty")
    @Size(max = 128, message = "targetName too long")
    private String targetName;
}
