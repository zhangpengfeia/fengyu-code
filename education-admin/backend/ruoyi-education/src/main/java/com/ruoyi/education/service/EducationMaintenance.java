package com.ruoyi.education.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Configure invokeTarget=educationMaintenance.expire() in native Quartz job management. */
@Component("educationMaintenance")
public class EducationMaintenance {
    final JdbcTemplate jdbc;
    public EducationMaintenance(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @Transactional public void expire() {
        remind();
        jdbc.update("UPDATE biz_school SET school_status='DISABLED',update_by='scheduler',update_time=CURRENT_TIMESTAMP WHERE del_flag='0' AND expire_time<=CURRENT_TIMESTAMP AND school_status='ACTIVE'");
        jdbc.update("UPDATE sys_dept SET status='1',update_by='scheduler',update_time=CURRENT_TIMESTAMP WHERE dept_id IN (SELECT school_id FROM biz_school WHERE del_flag='0' AND school_status='DISABLED')");
        jdbc.update("UPDATE biz_student_entitlement SET status='EXPIRED',update_by='scheduler',update_time=CURRENT_TIMESTAMP WHERE del_flag='0' AND status='ACTIVE' AND end_time<=CURRENT_TIMESTAMP");
        jdbc.update("UPDATE biz_activate_code SET status='2',update_by='scheduler',update_time=CURRENT_TIMESTAMP WHERE del_flag='0' AND status='0' AND batch_id IN (SELECT batch_id FROM biz_activate_code_batch WHERE expire_time<=CURRENT_TIMESTAMP OR del_flag<>'0')");
    }
    public void remind() {
        var zone=java.time.ZoneId.of("Asia/Shanghai");var today=java.time.LocalDate.now(zone);
        for(var school:jdbc.queryForList("SELECT s.*,d.ancestors FROM biz_school s JOIN sys_dept d ON d.dept_id=s.school_id WHERE s.del_flag='0' AND s.school_status='ACTIVE' AND s.expire_time>CURRENT_TIMESTAMP")) {
            var expires=AccessService.instant(school.get("expire_time"));long days=java.time.temporal.ChronoUnit.DAYS.between(today,expires.atZone(zone).toLocalDate());
            if(!java.util.Set.of(7L,3L,1L).contains(days))continue;
            jdbc.queryForList("SELECT school_id FROM biz_school WHERE school_id=? FOR UPDATE",school.get("school_id"));
            if(!jdbc.queryForList("SELECT id FROM biz_school_expiry_notice WHERE school_id=? AND expire_time=? AND days_before=?",school.get("school_id"),school.get("expire_time"),days).isEmpty())continue;
            var recipients=jdbc.queryForList("SELECT DISTINCT u.user_id FROM sys_user u JOIN sys_user_role ur ON ur.user_id=u.user_id JOIN sys_role r ON r.role_id=ur.role_id WHERE u.status='0' AND u.del_flag='0' AND r.status='0' AND r.del_flag='0' AND ((r.role_key='edu_school' AND u.dept_id=?) OR (r.role_key IN ('edu_region','admin') AND FIND_IN_SET(u.dept_id,?)>0))",Long.class,school.get("school_id"),school.get("ancestors"));
            if(recipients.isEmpty())continue;
            var key=new org.springframework.jdbc.support.GeneratedKeyHolder();
            String title="学校服务即将到期";String content="请在学校管理中查看您负责学校的到期提醒，并及时安排续期。";
            jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO sys_notice(notice_title,notice_type,notice_content,status,create_by,create_time) VALUES(?,'1',?,'0','education-scheduler',CURRENT_TIMESTAMP)",java.sql.Statement.RETURN_GENERATED_KEYS);statement.setString(1,title);statement.setString(2,content);return statement;},key);
            long notice=java.util.Objects.requireNonNull(key.getKey()).longValue();
            jdbc.update("INSERT INTO biz_school_expiry_notice(school_id,expire_time,days_before,notice_id,create_by) VALUES(?,?,?,?,'scheduler')",school.get("school_id"),school.get("expire_time"),days,notice);
            for(long uid:recipients)jdbc.update("INSERT INTO biz_notice_recipient(notice_id,user_id,create_by) VALUES(?,?,'scheduler')",notice,uid);
        }
    }
}
