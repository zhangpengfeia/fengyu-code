package com.ruoyi.education.config;

import com.ruoyi.education.domain.BizTable;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.education.service.GroupingService;
import com.ruoyi.system.service.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import java.util.Map;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EducationBootstrapTest {
    void verifyInitialPasswordRule(String profile, int mustChange) {
        var env = new MockEnvironment().withProperty("education.bootstrap-password", "admin123456");
        env.setActiveProfiles(profile);
        var users = mock(ISysUserService.class);
        var db = mock(BizMapper.class);
        new EducationBootstrap(users, db, env, mock(JdbcTemplate.class), mock(GroupingService.class))
            .run(new DefaultApplicationArguments(new String[0]));
        verify(db).insert(eq(BizTable.SECURITY), argThat(row ->
            ((Number) row.get("must_change_pwd")).intValue() == mustChange
                && ((Number) row.get("user_id")).longValue() == 1L), eq("bootstrap"));
    }
    @Test void disposableAdminCanLoginAfterRestart() { verifyInitialPasswordRule("dev", 0); }
    @Test void productionAdminStillRequiresInitialPasswordChange() { verifyInitialPasswordRule("education", 1); }
}
