package cn.skylark.iot.access.service;

import cn.skylark.iot.access.config.IotAccessProperties;
import cn.skylark.iot.access.model.DownstreamPublishRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * EMQX Management API client (方案1：通过 EMQX publish API 下行消息)。
 *
 * 参考：EMQX 5.x /api/v5/publish
 */
@Service
public class EmqxManagementHttpClient implements EmqxManagementClient {

    private static final Logger log = LoggerFactory.getLogger(EmqxManagementHttpClient.class);

    private final IotAccessProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmqxManagementHttpClient(IotAccessProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void checkCredentialHealthAtStartup() {
        String baseUrl = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            log.warn("EMQX credential health check skipped: baseUrl is empty.");
            return;
        }
        checkBasicAuthHealth(baseUrl);
        checkTokenLoginHealth(baseUrl);
    }

    @Override
    public Optional<String> publish(DownstreamPublishRequest req) {
        String baseUrl = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            return Optional.of("emqx baseUrl is empty");
        }
        String url = baseUrl.endsWith("/") ? (baseUrl + "api/v5/publish") : (baseUrl + "/api/v5/publish");

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("topic", req.getTopic());
        body.put("payload", req.getPayload() == null ? "" : req.getPayload());
        body.put("qos", req.getQos() == null ? 1 : req.getQos());
        body.put("retain", req.getRetain() != null && req.getRetain());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getApiKey());
        String apiSecret = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getApiSecret());
        if (StringUtils.hasText(apiKey) && StringUtils.hasText(apiSecret)) {
            headers.setBasicAuth(apiKey, apiSecret);
        } else {
            log.warn("EMQX apiKey/apiSecret empty; publish may fail if EMQX requires auth");
        }

        try {
            log.info("iot.downstream.publish traceId={}, topic={}, qos={}, retain={}",
                    req.getTraceId(),
                    req.getTopic(),
                    body.get("qos"),
                    body.get("retain"));
            try {
                return doPublish(url, body, headers);
            } catch (HttpStatusCodeException e) {
                if (e.getRawStatusCode() != 401) {
                    return Optional.of("emqx publish request failed: " + e.getStatusCode() + ": " + abbreviate(e.getResponseBodyAsString(), 300));
                }
                String token = loginDashboardAndGetToken(baseUrl);
                if (!StringUtils.hasText(token)) {
                    return Optional.of("emqx publish request failed: " + e.getStatusCode() + ": " + abbreviate(e.getResponseBodyAsString(), 300));
                }
                HttpHeaders bearerHeaders = new HttpHeaders();
                bearerHeaders.setContentType(MediaType.APPLICATION_JSON);
                bearerHeaders.setBearerAuth(token);
                log.info("iot.downstream.publish retry with dashboard token, traceId={}", req.getTraceId());
                return doPublish(url, body, bearerHeaders);
            }
        } catch (RestClientException e) {
            return Optional.of("emqx publish request failed: " + e.getMessage());
        } catch (Exception e) {
            return Optional.of("emqx publish unexpected error: " + e.getMessage());
        }
    }

    private Optional<String> doPublish(String url, Map<String, Object> body, HttpHeaders headers) throws Exception {
        ResponseEntity<String> resp = restTemplate.postForEntity(url, new HttpEntity<Map<String, Object>>(body, headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            return Optional.of("emqx publish http not 2xx: " + resp.getStatusCode());
        }
        String raw = resp.getBody() == null ? "" : resp.getBody();
        return parseErrorIfAny(raw);
    }

    private String loginDashboardAndGetToken(String baseUrl) {
        String username = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getDashboardUsername());
        String password = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getDashboardPassword());
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return "";
        }
        String loginUrl = baseUrl.endsWith("/") ? (baseUrl + "api/v5/login") : (baseUrl + "/api/v5/login");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, String> payload = new HashMap<String, String>();
            payload.put("username", username);
            payload.put("password", password);
            ResponseEntity<String> resp = restTemplate.postForEntity(loginUrl, new HttpEntity<Map<String, String>>(payload, headers), String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || !StringUtils.hasText(resp.getBody())) {
                return "";
            }
            JsonNode root = objectMapper.readTree(resp.getBody());
            return safe(root.path("token").asText(""));
        } catch (Exception e) {
            log.warn("EMQX dashboard login fallback failed: {}", e.getMessage());
            return "";
        }
    }

    private void checkBasicAuthHealth(String baseUrl) {
        String apiKey = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getApiKey());
        String apiSecret = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getApiSecret());
        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(apiSecret)) {
            log.warn("EMQX basic credential health check: EMQX_API_KEY/EMQX_API_SECRET is empty.");
            return;
        }
        String checkUrl = baseUrl.endsWith("/") ? (baseUrl + "api/v5/bridges") : (baseUrl + "/api/v5/bridges");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(apiKey, apiSecret);
            ResponseEntity<String> resp = restTemplate.exchange(checkUrl, org.springframework.http.HttpMethod.GET, new HttpEntity<String>(headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("EMQX basic credential health check passed.");
                return;
            }
            log.warn("EMQX basic credential health check failed: status={}", resp.getStatusCodeValue());
        } catch (HttpStatusCodeException e) {
            log.warn("EMQX basic credential health check failed: status={}, body={}",
                    e.getRawStatusCode(), abbreviate(e.getResponseBodyAsString(), 200));
        } catch (Exception e) {
            log.warn("EMQX basic credential health check error: {}", e.getMessage());
        }
    }

    private void checkTokenLoginHealth(String baseUrl) {
        String username = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getDashboardUsername());
        String password = properties.getEmqx() == null ? "" : safe(properties.getEmqx().getDashboardPassword());
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            log.warn("EMQX token credential health check: dashboard username/password is empty.");
            return;
        }
        String token = loginDashboardAndGetToken(baseUrl);
        if (StringUtils.hasText(token)) {
            log.info("EMQX token credential health check passed for user '{}'.", username);
            return;
        }
        log.warn("EMQX token credential health check failed for user '{}'.", username);
    }

    private Optional<String> parseErrorIfAny(String raw) throws Exception {
        if (!StringUtils.hasText(raw)) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(raw);
        if (root.has("code")) {
            String code = root.path("code").asText("");
            if (!"0".equals(code) && !"SUCCESS".equalsIgnoreCase(code) && !"200".equals(code)) {
                return Optional.of("emqx publish failed, code=" + code + ", body=" + abbreviate(raw, 600));
            }
        }
        if (root.has("error")) {
            return Optional.of("emqx publish failed, error=" + abbreviate(root.path("error").toString(), 600));
        }
        return Optional.empty();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}

