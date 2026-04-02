package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;
import javax.validation.constraints.AssertTrue;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import cn.skylark.iot.mgmt.model.enums.ProductProtocolType;
import cn.skylark.iot.mgmt.model.enums.DeviceType;

@Data
public class CreateProductRequest {
    @NotBlank(message = "productKey cannot be empty")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{2,64}$", message = "productKey format invalid")
    private String productKey;

    @NotBlank(message = "name cannot be empty")
    @Size(max = 128, message = "name too long")
    private String name;

    @Size(max = 512, message = "coverImageUrl too long")
    private String coverImageUrl;

    @Size(max = 512, message = "thumbnailUrl too long")
    private String thumbnailUrl;

    @Size(max = 512, message = "description too long")
    private String description;

    private String protocolType;
    private String deviceType;

    @AssertTrue(message = "protocolType invalid")
    public boolean isProtocolTypeValid() {
        return ProductProtocolType.isValid(protocolType);
    }

    @AssertTrue(message = "deviceType invalid")
    public boolean isDeviceTypeValid() {
        return DeviceType.isValid(deviceType);
    }
}

