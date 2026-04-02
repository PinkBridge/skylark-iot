package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CreateDeviceConnectRecordRequest {
    @Pattern(regexp = "^(connected|disconnected)$", message = "action invalid")
    private String action;

    @Size(max = 128, message = "clientId too long")
    private String clientId;

    @Size(max = 64, message = "ip too long")
    private String ip;

    @Size(max = 255, message = "userAgent too long")
    private String userAgent;
}

