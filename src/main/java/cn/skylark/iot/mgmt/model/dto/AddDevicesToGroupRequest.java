package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class AddDevicesToGroupRequest {
    @NotEmpty(message = "devices required")
    private List<@Valid DeviceGroupMemberRequest> devices;
}

