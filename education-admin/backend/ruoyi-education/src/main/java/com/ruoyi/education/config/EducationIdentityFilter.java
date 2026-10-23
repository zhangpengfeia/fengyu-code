package com.ruoyi.education.config;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.education.service.AccessService;
import com.ruoyi.education.domain.BizTable;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

/** Runs inside the servlet chain after Spring Security established the JWT principal. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE-100)
public class EducationIdentityFilter extends OncePerRequestFilter {
    final AccessService access;final BizMapper db;final TokenService tokens;final SysPermissionService permissions;final ObjectMapper json;
    final StringRedisTemplate redis;
    public EducationIdentityFilter(AccessService a,BizMapper db,TokenService t,SysPermissionService p,ObjectMapper j,StringRedisTemplate redis) {access=a;this.db=db;tokens=t;permissions=p;json=j;this.redis=redis;}
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        var authentication=SecurityUtils.getAuthentication();
        if(authentication==null || !(authentication.getPrincipal() instanceof LoginUser user)) {chain.doFilter(req,res);return;}
        try {
            var fresh=access.active(user.getUserId());var security=db.one(BizTable.SECURITY,user.getUserId());
            String key="edu:credential:"+user.getToken();
            if(!Objects.equals(redis.opsForValue().get(key),fresh.getPassword())) throw new ServiceException("密码或会话已更新，请重新登录",401);
            redis.expire(key,Duration.ofMinutes(35));user.setUser(fresh);user.setPermissions(permissions.getMenuPermission(fresh));
            String path=req.getRequestURI();
            boolean must=security!=null && AccessService.id(security.get("must_change_pwd"))==1;
            if(must && !Set.of("/getInfo","/getRouters","/logout","/biz/auth/me","/biz/auth/password").contains(path)) throw new ServiceException("请先修改初始密码",428);
            chain.doFilter(req,res);
        } catch(ServiceException ex) {
            int status=ex.getCode()==null?403:ex.getCode();res.setStatus(status);res.setContentType("application/json;charset=UTF-8");
            json.writeValue(res.getWriter(),Map.of("code",status,"msg",ex.getMessage()));
        }
    }
}
