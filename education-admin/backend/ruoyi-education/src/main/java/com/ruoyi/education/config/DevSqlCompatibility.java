package com.ruoyi.education.config;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.sql.Connection;

/** Only the in-memory developer profile adapts the native MySQL clock function. */
@Component @Profile("dev")
@Intercepts(@Signature(type=StatementHandler.class,method="prepare",args={Connection.class,Integer.class}))
public class DevSqlCompatibility implements Interceptor {
    @Override public Object intercept(Invocation invocation) throws Throwable {
        var meta=SystemMetaObject.forObject(invocation.getTarget());
        String sql=(String)meta.getValue("delegate.boundSql.sql");
        meta.setValue("delegate.boundSql.sql",sql.replaceAll("(?i)sysdate\\(\\)","CURRENT_TIMESTAMP"));
        return invocation.proceed();
    }
}
