package com.ruoyi.education.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AgentSessionServiceTest {
    final String key = "separate-service-key-" + "x".repeat(32);
    final ObjectMapper json = new ObjectMapper();
    AccessService access;
    HttpClient http;
    SysUser student;

    @BeforeEach void setup() {
        access = mock(AccessService.class);
        http = mock(HttpClient.class);
        student = new SysUser(20002L);
        student.setDeptId(11000L);
        student.setNickName("测试同学");
        when(access.student(student)).thenReturn(true);
        when(access.current()).thenReturn(student);
    }

    AgentSessionService service(String base, String secret) {
        return new AgentSessionService(access, json, base, secret, http);
    }

    @SuppressWarnings("unchecked")
    void reply(int status, String body) throws Exception {
        HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenAnswer(invocation -> new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
        when(http.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    @Test void forwardsOnlyServerConfirmedStudentAndKeepsServiceKeyPrivate() throws Exception {
        String token = "signed-student-token-" + "t".repeat(32);
        reply(200, json.writeValueAsString(java.util.Map.of("access_token", token, "expires_in", 900, "demo", true)));
        var result = service("http://agent-api:8088", key).currentSession();
        var request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertEquals("http://agent-api:8088/internal/v1/tokens", request.getValue().uri().toString());
        assertEquals(key, request.getValue().headers().firstValue("X-Integration-Key").orElseThrow());
        var facts = json.readTree(readBody(request.getValue()));
        assertEquals("20002", facts.path("student_id").asText());
        assertEquals("11000", facts.path("school_id").asText());
        assertEquals(2, facts.size());
        assertEquals(token, result.get("access_token"));
        assertEquals(true, result.get("demo"));
        assertEquals("测试同学", ((java.util.Map<?, ?>) result.get("student")).get("name"));
        assertFalse(json.writeValueAsString(result).contains(key));
    }

    @Test void demoIndicatorAcceptsOnlyExplicitBooleanFromTheAuthenticatedService() throws Exception {
        String token = "signed-student-token-" + "t".repeat(32);
        for (String value : new String[]{"\"true\"", "false", "null"}) {
            reply(200, "{\"access_token\":\"" + token + "\",\"expires_in\":900,\"demo\":" + value + "}");
            assertEquals(false, service("http://agent-api:8088", key).session(student).get("demo"));
        }
    }

    @Test void teachersCannotObtainStudentAgentTokens() {
        when(access.student(student)).thenReturn(false);
        var error = assertThrows(ServiceException.class, () -> service("http://agent-api:8088", key).session(student));
        assertEquals(403, error.getCode());
        verifyNoInteractions(http);
    }

    @Test void unavailableAndInvalidConfigurationsFailWithoutCallingUpstream() {
        for (String base : new String[]{"", "https://user:pass@agent-api", "http://agent-api/path", "file:///tmp/key", "http://agent-api?q=secret"}) {
            var error = assertThrows(ServiceException.class, () -> service(base, key).session(student));
            assertEquals(503, error.getCode());
        }
        assertThrows(ServiceException.class, () -> service("http://agent-api:8088", "").session(student));
        verifyNoInteractions(http);
    }

    @Test void upstreamFailuresDoNotExposeInternalBodiesOrCredentials() throws Exception {
        reply(401, "upstream error containing " + key);
        var error = assertThrows(ServiceException.class, () -> service("http://agent-api:8088", key).session(student));
        assertEquals(503, error.getCode());
        assertFalse(error.getMessage().contains(key));
        assertFalse(error.getMessage().contains("agent-api"));
    }

    @Test void malformedAndOverlongUpstreamResponsesFailClosed() throws Exception {
        for (String body : new String[]{"", "not-json", "{}", "{\"access_token\":\"short\",\"expires_in\":900}", "x".repeat(17000)}) {
            reply(200, body);
            assertEquals(503, assertThrows(ServiceException.class,
                () -> service("http://agent-api:8088", key).session(student)).getCode());
        }
    }

    private String readBody(HttpRequest request) throws Exception {
        var text = new StringBuilder();
        var done = new CompletableFuture<String>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer bytes) { text.append(StandardCharsets.UTF_8.decode(bytes)); }
            public void onError(Throwable error) { done.completeExceptionally(error); }
            public void onComplete() { done.complete(text.toString()); }
        });
        return done.get();
    }
}
