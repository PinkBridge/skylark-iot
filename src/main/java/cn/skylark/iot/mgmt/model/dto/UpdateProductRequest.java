package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;
import javax.validation.constraints.AssertTrue;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import cn.skylark.iot.mgmt.model.enums.ProductProtocolType;
import cn.skylark.iot.mgmt.model.enums.DeviceType;

@Data
public class UpdateProductRequest {
    @NotBlank(message = "name cannot be empty")
    @Size(max = 128, message = "name too long")
    private String name;

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

