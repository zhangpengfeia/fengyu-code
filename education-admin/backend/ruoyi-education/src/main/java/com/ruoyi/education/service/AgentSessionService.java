package com.ruoyi.education.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Issues a short-lived Agent token using only the current server-confirmed student. */
@Service
public class AgentSessionService {
    private final AccessService access;
    private final ObjectMapper json;
    private final String baseUrl;
    private final String serviceKey;
    private final HttpClient http;

    @Autowired
    public AgentSessionService(AccessService access, ObjectMapper json,
            @Value("${education.agent-base-url:${EDU_AGENT_BASE_URL:}}") String baseUrl,
            @Value("${education.agent-service-api-key:${EDU_AGENT_SERVICE_API_KEY:}}") String serviceKey) {
        this(access, json, baseUrl, serviceKey, HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build());
    }

    AgentSessionService(AccessService access, ObjectMapper json, String baseUrl,
            String serviceKey, HttpClient http) {
        this.access = access;
        this.json = json;
        this.baseUrl = baseUrl.trim();
        this.serviceKey = serviceKey;
        this.http = http;
    }

    public Map<String, Object> currentSession() { return session(access.current()); }

    public Map<String, Object> session(SysUser student) {
        if (!access.student(student) || student.getDeptId() == null || student.getDeptId() <= 0)
            throw new ServiceException("智能学伴仅向学生开放，请使用学生账号登录", 403);
        URI endpoint = endpoint();
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("X-Integration-Key", serviceKey)
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of(
                    "student_id", student.getUserId().toString(),
                    "school_id", student.getDeptId().toString())))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                if (response.statusCode() != 200) throw unavailable();
                byte[] content = body.readNBytes(16385);
                if (content.length > 16384) throw unavailable();
                var result = json.readTree(content);
                if (result == null) throw unavailable();
                var token = result.path("access_token");
                var ttl = result.path("expires_in");
                if (!token.isTextual() || token.asText().length() < 32 || token.asText().length() > 8192
                        || !ttl.isIntegralNumber() || ttl.asLong() < 1 || ttl.asLong() > 3600)
                    throw unavailable();
                return Map.of("access_token", token.asText(), "expires_in", ttl.asInt(),
                    "demo", result.path("demo").isBoolean() && result.path("demo").asBoolean(),
                    "student", Map.of("id", student.getUserId().toString(),
                        "name", student.getNickName() == null ? "同学" : student.getNickName(),
                        "school_id", student.getDeptId().toString()));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException | IllegalArgumentException ex) {
            // Do not expose upstream bodies, service credentials, or internal addresses.
            throw unavailable();
        }
    }

    private URI endpoint() {
        try {
            URI base = URI.create(baseUrl);
            if (serviceKey.length() < 32 || base.getHost() == null
                    || !("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                    || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null
                    || !(base.getPath().isEmpty() || "/".equals(base.getPath())))
                throw new IllegalArgumentException();
            return base.resolve("/internal/v1/tokens");
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("智能学伴身份服务尚未配置，请联系管理员", 503);
        }
    }

    private ServiceException unavailable() {
        return new ServiceException("学伴身份服务暂不可用，请稍后重试", 503);
    }
}
