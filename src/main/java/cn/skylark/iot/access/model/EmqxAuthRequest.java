package cn.skylark.iot.access.model;

import lombok.Data;

@Data
public class EmqxAuthRequest {
    private String clientid;
    private String username;
    private String password;
}
