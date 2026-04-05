package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class UpdateProductDataChannelRequest {
    @NotNull(message = "enabled required")
    private Boolean enabled;

    @Pattern(regexp = "^(allow|deny)$", message = "effect invalid")
    private String effect;
}
