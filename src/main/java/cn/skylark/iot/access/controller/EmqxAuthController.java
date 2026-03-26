package cn.skylark.iot.access.controller;

import cn.skylark.iot.access.model.EmqxAclRequest;
import cn.skylark.iot.access.model.EmqxAuthRequest;
import cn.skylark.iot.access.model.EmqxAuthResponse;
import cn.skylark.iot.access.model.EmqxAuthzResponse;
import cn.skylark.iot.access.service.DeviceAccessAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * EMQX HTTP 鉴权/ACL 回调接口（MVP）。
 */
@RestController
@RequestMapping("/api/access/emqx")
public class EmqxAuthController {

    private final DeviceAccessAuthService authService;

    public EmqxAuthController(DeviceAccessAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth")
    public ResponseEntity<EmqxAuthResponse> auth(@RequestBody(required = false) EmqxAuthRequest body,
                                                @RequestParam(value = "username", required = false) String qUsername,
                                                @RequestParam(value = "password", required = false) String qPassword) {
        String username = body != null ? body.getUsername() : qUsername;
        String password = body != null ? body.getPassword() : qPassword;
        boolean ok = authService.authenticate(username, password);
        // EMQX 5.x：必须返回 application/json，并用 body.result 表示 allow/deny/ignore。
        return ResponseEntity.ok(ok ? EmqxAuthResponse.allow() : EmqxAuthResponse.deny());
    }

    @PostMapping("/acl")
    public ResponseEntity<EmqxAuthzResponse> acl(@RequestBody(required = false) EmqxAclRequest body,
                                                @RequestParam(value = "username", required = false) String qUsername,
                                                @RequestParam(value = "action", required = false) String qAction,
                                                @RequestParam(value = "topic", required = false) String qTopic) {
        String username = body != null ? body.getUsername() : qUsername;
        String action = body != null ? body.getAction() : qAction;
        String topic = body != null ? body.getTopic() : qTopic;
        boolean ok = authService.allowAcl(username, action, topic);
        // EMQX 5.x Authorization：status=200，body.result=allow/deny/ignore。
        return ResponseEntity.ok(ok ? EmqxAuthzResponse.allow() : EmqxAuthzResponse.deny());
    }
}
