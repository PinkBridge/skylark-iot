package cn.skylark.iot.access.controller;

import cn.skylark.iot.access.config.IotAccessProperties;
import cn.skylark.iot.access.model.EmqxClientSessionEvent;
import cn.skylark.iot.access.service.EmqxSessionWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/**
 * EMQX 规则引擎 Webhook：客户端上下线写入 {@code iot_device_connect_record}。
 * <p>
 * 鉴权：请求头 {@value #HEADER_WEBHOOK_SECRET} 须与 {@code iot.access.webhook.secret} 一致。
 */
@RestController
@RequestMapping("/api/access/emqx/webhook")
public class EmqxWebhookController {

    public static final String HEADER_WEBHOOK_SECRET = "X-Emqx-Webhook-Secret";
    private static final Logger log = LoggerFactory.getLogger(EmqxWebhookController.class);

    private final IotAccessProperties properties;
    private final EmqxSessionWebhookService sessionWebhookService;
    private final ObjectMapper objectMapper;

    public EmqxWebhookController(IotAccessProperties properties,
                               EmqxSessionWebhookService sessionWebhookService,
                               ObjectMapper objectMapper) {
        this.properties = properties;
        this.sessionWebhookService = sessionWebhookService;
        this.objectMapper = objectMapper;
    }

    /**
     * 从 {@link HttpServletRequest} 读原始 body 再解析 JSON，不经过 {@code @RequestBody}，
     * 避免 EMQX 使用 {@code Content-Type: application/octet-stream} 时触发 {@code HttpMediaTypeNotSupportedException}。
     */
    @PostMapping("/session")
    public ResponseEntity<Map<String, Object>> session(
            @RequestHeader(value = HEADER_WEBHOOK_SECRET, required = false) String secret,
            HttpServletRequest request) {
        log.info("emqx webhook received: contentType={}, remoteAddr={}, hasSecretHeader={}",
                request.getContentType(), request.getRemoteAddr(), StringUtils.hasText(secret));
        IotAccessProperties.Webhook wh = properties.getWebhook();
        if (wh == null || !wh.isEnabled()) {
            log.warn("emqx webhook blocked: webhook disabled");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "webhook disabled");
        }
        if (!StringUtils.hasText(wh.getSecret())) {
            log.warn("emqx webhook blocked: webhook secret not configured");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "webhook secret not configured");
        }
        if (!StringUtils.hasText(secret) || !wh.getSecret().equals(secret.trim())) {
            log.warn("emqx webhook blocked: invalid webhook secret, headerPresent={}", StringUtils.hasText(secret));
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid webhook secret");
        }

        byte[] rawBody;
        try {
            rawBody = StreamUtils.copyToByteArray(request.getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot read body");
        }
        log.info("emqx webhook body received: bytes={}", rawBody.length);
        EmqxClientSessionEvent body = null;
        if (rawBody.length > 0) {
            try {
                body = objectMapper.readValue(rawBody, EmqxClientSessionEvent.class);
                log.info("emqx webhook parsed: event={}, username={}, clientid={}, reason={}",
                        body.getEvent(), body.getUsername(), body.getClientid(), body.getReason());
            } catch (Exception e) {
                log.warn("emqx webhook json parse failed: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid json body");
            }
        }

        boolean processed = sessionWebhookService.handleSessionEvent(body);
        log.info("emqx webhook handled: processed={}", processed);
        return ResponseEntity.ok(Collections.singletonMap("ok", processed));
    }
}
