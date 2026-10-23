package com.ruoyi.education.config;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.education.service.AccessService;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.education.domain.BizTable;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import java.util.Objects;
import com.ruoyi.common.core.redis.RedisCache;
import java.time.Duration;

@Component
public class LoginPolicy {
    final AccessService access; final BizMapper db; final RedisCache redis;
    public LoginPolicy(AccessService access,BizMapper db,RedisCache redis) {this.access=access;this.db=db;this.redis=redis;}
    @EventListener public void validate(LoginUser user) {
        var fresh=access.active(user.getUserId());
        redis.set("edu:credential:"+user.getToken(),fresh.getPassword(),Duration.ofMinutes(35));
    }
}
