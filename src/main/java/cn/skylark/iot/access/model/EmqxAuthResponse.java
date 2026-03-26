package cn.skylark.iot.access.model;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * EMQX 5.x HTTP Auth 响应格式：
 * - content-type: application/json
 * - result: allow | deny | ignore
 * - is_superuser: true | false
 */
@Data
public class EmqxAuthResponse {
    private String result;
    @JsonProperty("is_superuser")
    private boolean superuser;

    public static EmqxAuthResponse allow() {
        EmqxAuthResponse r = new EmqxAuthResponse();
        r.result = "allow";
        r.superuser = false;
        return r;
    }

    public static EmqxAuthResponse deny() {
        EmqxAuthResponse r = new EmqxAuthResponse();
        r.result = "deny";
        r.superuser = false;
        return r;
    }
}

