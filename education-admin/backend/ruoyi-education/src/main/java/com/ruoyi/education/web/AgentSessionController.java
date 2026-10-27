package com.ruoyi.education.web;

import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.service.AgentSessionService;
import com.ruoyi.education.service.GameService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AgentSessionController {
    private final GameService games;
    private final AgentSessionService sessions;
    private final String gameOrigin;

    public AgentSessionController(GameService games, AgentSessionService sessions,
            @Value("${education.game-origin:http://localhost:5173}") String gameOrigin) {
        this.games = games;
        this.sessions = sessions;
        this.gameOrigin = gameOrigin.replaceAll("/$", "");
    }

    @Anonymous
    @PostMapping("/play/planet/agent-session")
    public ResponseEntity<?> planet(HttpServletRequest request,
            @RequestBody(required = false) String body) {
        // The game cookie is intentionally scoped to /play/planet/ and never exposed to JS.
        if (!gameOrigin.equals(request.getHeader("Origin")))
            throw new ServiceException("请从当前星球页面打开智能学伴", 403);
        rejectClientIdentity(request, body);
        String sid = null;
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies())
            if ("EDU_GAME".equals(cookie.getName())) sid = cookie.getValue();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(sessions.session(games.sessionUser("planet", sid)));
    }

    @PostMapping("/biz/auth/agent-token")
    public ResponseEntity<?> current(HttpServletRequest request,
            @RequestBody(required = false) String body) {
        rejectClientIdentity(request, body);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(AjaxResult.success(sessions.currentSession()));
    }

    private void rejectClientIdentity(HttpServletRequest request, String body) {
        if ((body != null && !body.isBlank()) || request.getQueryString() != null)
            throw new ServiceException("学伴身份由服务端确认，不接受浏览器提交身份或成绩", 400);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<?> error(ServiceException exception) {
        int status = exception.getCode() == null ? 403 : exception.getCode();
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
            .body(Map.of("code", status, "msg", exception.getMessage()));
    }
}
