package com.ruoyi.education.config;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.URI;

/** Fail closed when production cookies would be shared with untrusted game code. */
@Component @Order(-100)
public class GameOriginPolicy implements ApplicationRunner {
    final Environment env;
    public GameOriginPolicy(Environment e){env=e;}
    @Override public void run(ApplicationArguments args) {
        if(env.matchesProfiles("dev"))return;
        URI admin=URI.create(env.getRequiredProperty("education.admin-origin"));
        URI game=URI.create(env.getRequiredProperty("education.game-origin"));
        if(!"https".equals(admin.getScheme())||!"https".equals(game.getScheme())||admin.getHost()==null||game.getHost()==null||admin.getHost().equalsIgnoreCase(game.getHost())||!env.getProperty("education.secure-game-cookie",Boolean.class,true))
            throw new IllegalStateException("Production requires distinct HTTPS admin/game hostnames and Secure game cookies");
    }
}
