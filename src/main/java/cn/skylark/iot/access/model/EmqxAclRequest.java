package cn.skylark.iot.access.model;

import lombok.Data;

@Data
public class EmqxAclRequest {
    private String clientid;
    private String username;
    /**
     * subscribe / publish
     */
    private String action;
    private String topic;
}
