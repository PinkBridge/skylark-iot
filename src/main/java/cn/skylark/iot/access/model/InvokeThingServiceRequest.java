package cn.skylark.iot.access.model;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
public class InvokeThingServiceRequest {
    @NotBlank(message = "productKey cannot be empty")
    private String productKey;

    @NotBlank(message = "deviceName cannot be empty")
    private String deviceName;

    @NotBlank(message = "identifier cannot be empty")
    private String identifier;

    /**
     * Alink service input args JSON string. Example: {"targetTemp":26.0}
     */
    private String args;

    @Min(value = 0, message = "qos must be 0,1,2")
    @Max(value = 2, message = "qos must be 0,1,2")
    private Integer qos = 1;

    /**
     * true: wait service reply and return Result; false: return immediately.
     */
    private Boolean sync = false;

    /**
     * Effective only when sync=true.
     */
    @Min(value = 1000, message = "timeoutMs must be >= 1000")
    @Max(value = 30000, message = "timeoutMs must be <= 30000")
    private Integer timeoutMs = 8000;
}

