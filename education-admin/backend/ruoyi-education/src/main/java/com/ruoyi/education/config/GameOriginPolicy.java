package com.ruoyi.education.config;
import org.springframework.boot.*;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.net.URI;

/** Same-origin deployment is supported for trusted internal games; production still requires HTTPS. */
@Component @Order(-100)
public class GameOriginPolicy implements ApplicationRunner {
    final Environment env;
    public GameOriginPolicy(Environment e){env=e;}
    @Override public void run(ApplicationArguments args) {
        if(env.matchesProfiles("dev"))return;
        URI admin=URI.create(env.getRequiredProperty("education.admin-origin"));
        URI game=URI.create(env.getRequiredProperty("education.game-origin"));
        if(!httpsOrigin(admin)||!httpsOrigin(game)||!env.getProperty("education.secure-game-cookie",Boolean.class,true))
            throw new IllegalStateException("Production requires HTTPS admin/game origins and Secure game cookies");
    }
    private boolean httpsOrigin(URI origin) {
        return "https".equals(origin.getScheme()) && origin.getHost()!=null && origin.getRawUserInfo()==null
            && origin.getRawQuery()==null && origin.getRawFragment()==null
            && (origin.getRawPath()==null || origin.getRawPath().isEmpty() || "/".equals(origin.getRawPath()));
    }
}
