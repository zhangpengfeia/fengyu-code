package com.ruoyi.education;

import com.ruoyi.education.config.GameOriginPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.DefaultApplicationArguments;
import static org.junit.jupiter.api.Assertions.*;

class GameOriginPolicyTest {
    private MockEnvironment env(String admin, String game, boolean secure) {
        return new MockEnvironment().withProperty("education.admin-origin",admin)
            .withProperty("education.game-origin",game)
            .withProperty("education.secure-game-cookie",Boolean.toString(secure));
    }
    private void start(MockEnvironment env) { new GameOriginPolicy(env).run(new DefaultApplicationArguments()); }
    @Test void sameHttpsOriginIsSupported() {
        assertDoesNotThrow(()->start(env("https://fengyucode.zhangff.cn","https://fengyucode.zhangff.cn",true)));
    }
    @Test void separateHttpsOriginsRemainSupported() {
        assertDoesNotThrow(()->start(env("https://admin.example.com","https://games.example.com",true)));
    }
    @Test void productionDoesNotAcceptHttpOrInsecureCookies() {
        assertThrows(IllegalStateException.class,()->start(env("http://example.com","https://example.com",true)));
        assertThrows(IllegalStateException.class,()->start(env("https://example.com","http://example.com",true)));
        assertThrows(IllegalStateException.class,()->start(env("https://example.com","https://example.com",false)));
    }
    @Test void configuredOriginsCannotContainCredentialsPathsQueriesOrFragments() {
        for(String value:new String[]{"https://u:p@example.com","https://example.com/play","https://example.com?q=1","https://example.com/#fragment"})
            assertThrows(IllegalStateException.class,()->start(env("https://example.com",value,true)));
    }
    @Test void localDevelopmentStillAllowsHttp() {
        var local=env("http://localhost:5173","http://localhost:5173",false);local.setActiveProfiles("dev");
        assertDoesNotThrow(()->start(local));
    }
}
