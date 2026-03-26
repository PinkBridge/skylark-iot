package cn.skylark.iot.access.model;

import lombok.Data;

/**
 * EMQX 5.x HTTP Authorization 响应格式：
 * - content-type: application/json
 * - result: allow | deny | ignore
 */
@Data
public class EmqxAuthzResponse {
    private String result;

    public static EmqxAuthzResponse allow() {
        EmqxAuthzResponse r = new EmqxAuthzResponse();
        r.result = "allow";
        return r;
    }

    public static EmqxAuthzResponse deny() {
        EmqxAuthzResponse r = new EmqxAuthzResponse();
        r.result = "deny";
        return r;
    }
}

