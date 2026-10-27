package com.ruoyi.education;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.service.AgentSessionService;
import com.ruoyi.education.service.GameService;
import com.ruoyi.education.web.AgentSessionController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AgentSessionControllerTest {
    GameService games;
    AgentSessionService sessions;
    MockMvc mvc;
    final String origin = "https://fengyucode.zhangff.cn";
    final String sid = "s".repeat(43);
    SysUser student;

    @BeforeEach void setup() {
        games = mock(GameService.class);
        sessions = mock(AgentSessionService.class);
        student = new SysUser(20002L);
        mvc = MockMvcBuilders.standaloneSetup(new AgentSessionController(games, sessions, origin)).build();
        when(games.sessionUser("planet", sid)).thenReturn(student);
        when(sessions.session(student)).thenReturn(Map.of("access_token", "short-lived-student-token",
            "expires_in", 900, "student", Map.of("id", "20002", "name", "测试同学", "school_id", "11000")));
    }

    @Test void gameCookieResolvesServerIdentityAndResponseIsNeverCached() throws Exception {
        mvc.perform(post("/play/planet/agent-session").header("Origin", origin).cookie(new Cookie("EDU_GAME", sid)))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.student.id").value("20002"))
            .andExpect(jsonPath("$.access_token").value("short-lived-student-token"));
        verify(games).sessionUser("planet", sid);
        verify(sessions).session(student);
    }

    @Test void foreignOrMissingOriginsCannotUseCookieAuthentication() throws Exception {
        for (String candidate : new String[]{"https://attacker.example", "null", ""}) {
            mvc.perform(post("/play/planet/agent-session").header("Origin", candidate).cookie(new Cookie("EDU_GAME", sid)))
                .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/json"));
        }
        verifyNoInteractions(games, sessions);
    }

    @Test void browserSuppliedIdentityAndScoresAreRejected() throws Exception {
        mvc.perform(post("/play/planet/agent-session").header("Origin", origin).contentType("application/json")
                .content("{\"student_id\":\"another-student\",\"school_id\":\"other-school\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/play/planet/agent-session?student_id=another").header("Origin", origin))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(games, sessions);
    }

    @Test void expiredGameSessionCannotMintAgentToken() throws Exception {
        when(games.sessionUser(eq("planet"), isNull())).thenThrow(new ServiceException("游戏会话已过期", 403));
        mvc.perform(post("/play/planet/agent-session").header("Origin", origin))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.msg").value("游戏会话已过期"));
        verifyNoInteractions(sessions);
    }

    @Test void unavailableIntegrationReturnsExplicitJsonError() throws Exception {
        when(sessions.session(student)).thenThrow(new ServiceException("智能学伴身份服务尚未配置，请联系管理员", 503));
        mvc.perform(post("/play/planet/agent-session").header("Origin", origin).cookie(new Cookie("EDU_GAME", sid)))
            .andExpect(status().isServiceUnavailable()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.msg").value("智能学伴身份服务尚未配置，请联系管理员"));
    }
}
