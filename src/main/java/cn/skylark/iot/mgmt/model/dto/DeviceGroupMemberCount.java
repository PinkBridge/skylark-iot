package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class DeviceGroupMemberCount {
    private String groupKey;
    private long memberCount;
}
