package com.ruoyi.education.config;

import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.education.service.AccountService;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/** First-run administrator comes from environment; never resets an existing account. */
@Component
public class EducationBootstrap implements ApplicationRunner {
    final ISysUserService users;final BizMapper db;final Environment env;final JdbcTemplate jdbc;final com.ruoyi.education.service.GroupingService grouping;
    public EducationBootstrap(ISysUserService u,BizMapper db,Environment e,JdbcTemplate jdbc,com.ruoyi.education.service.GroupingService grouping){users=u;this.db=db;env=e;this.jdbc=jdbc;this.grouping=grouping;}
    @Override @Transactional public void run(ApplicationArguments args) {
        if(env.matchesProfiles("dev")&&!env.getProperty("education.dev-captcha-enabled",Boolean.class,true)) {
            jdbc.update("UPDATE sys_config SET config_value='false' WHERE config_key='sys.account.captchaEnabled'");
            // Only the explicit disposable UI-testing profile may disable the captcha.
            // Normal development and production keep the native captcha setting.
            com.ruoyi.common.utils.spring.SpringUtils.getBean(com.ruoyi.system.service.ISysConfigService.class).resetConfigCache();
        }
        if(users.selectUserById(1L)==null) {
            String password=env.getProperty("education.bootstrap-password");
            AccountService.password(password);
            user(1,"admin","系统管理员",100,1,password,true,null);
        }
        if(!env.matchesProfiles("dev")||env.getProperty("education.demo-password","").isBlank())return;
        String password=env.getProperty("education.demo-password");AccountService.password(password);
        if(db.one(BizTable.SCHOOL,11000L)!=null)return;
        jdbc.update("INSERT INTO sys_dept(dept_id,parent_id,ancestors,dept_name,order_num,status,del_flag,create_by,create_time) VALUES(11000,100,'0,100','智慧教育示范学校',1,'0','0','dev',CURRENT_TIMESTAMP)");
        var school=new LinkedHashMap<String,Object>(Map.of("school_id",11000L,"school_code","DEMO","school_name","智慧教育示范学校","expire_time",Date.from(Instant.now().plus(Duration.ofDays(365))),"school_status","ACTIVE"));db.insert(BizTable.SCHOOL,school,"dev");
        db.insert(BizTable.CLASS,new LinkedHashMap<>(Map.of("school_id",11000L,"academic_year","2026-2027","grade",1,"class_name","一年级一班","status","ACTIVE")),"dev");
        long cid=((Number)db.list(new BizQuery(BizTable.CLASS).eq("school_id",11000L)).get(0).get("class_id")).longValue();
        user(20001,"teacher","示范老师",11000,102,password,false,"13800000001");
        user(20002,"student","示范学生",11000,103,password,false,"13800000002");
        user(20003,"schooladmin","学校管理员",11000,101,password,false,null);
        db.insert(BizTable.STUDENT,new LinkedHashMap<>(Map.of("user_id",20002L,"school_id",11000L,"class_id",cid,"student_no","0001","is_trial",0)),"dev");
        grouping.join(20002L,cid,"dev");
        db.insert(BizTable.TEACHER,new LinkedHashMap<>(Map.of("teacher_user_id",20001L,"school_id",11000L,"class_id",cid,"subject_code","ANY","start_time",new Date())),"dev");
        db.insert(BizTable.POOL,new LinkedHashMap<>(Map.of("school_id",11000L,"subject_code","ANY","start_time",new Date(),"end_time",Date.from(Instant.now().plus(Duration.ofDays(365))))),"dev");
        db.insert(BizTable.COURSEWARE,new LinkedHashMap<>(Map.of("subject_code","MATH","volume",1,"stage_code","BASIC","courseware_name","认识数字与数量","description","观察、配对、探索数字。老师可从关联游戏进入星球教学；学生只能访问个人已授权内容。","status","PUBLISHED","version_no",1,"sort_no",1)),"dev");
        long cw=((Number)db.list(new BizQuery(BizTable.COURSEWARE)).get(0).get("courseware_id")).longValue();
        db.insert(BizTable.MATH,new LinkedHashMap<>(Map.of("courseware_id",cw,"stage","BASIC","ability","NUMBER")),"dev");
        db.linkGame(cw,1,1);db.linkGame(cw,2,2);db.version(cw,1,"{\"source\":\"development-fixture\"}","dev");
        var grant=new LinkedHashMap<String,Object>(Map.of("student_user_id",20002L,"source_type","MANUAL","status","ACTIVE","start_time",new Date(),"end_time",Date.from(Instant.now().plus(Duration.ofDays(180))),"remark","开发样例课件授权"));
        db.insert(BizTable.GRANT,grant,"dev");db.scope(((Number)grant.get("generatedId")).longValue(),Map.of("courseware_id",cw));
        var gameGrant=new LinkedHashMap<String,Object>(Map.of("student_user_id",20002L,"source_type","MANUAL","status","ACTIVE","start_time",new Date(),"end_time",Date.from(Instant.now().plus(Duration.ofDays(180))),"remark","开发样例游戏授权"));
        db.insert(BizTable.GRANT,gameGrant,"dev");db.scope(((Number)gameGrant.get("generatedId")).longValue(),Map.of("game_id",2L));
    }
    void user(long uid,String name,String nick,long school,long role,String password,boolean must,String phone) {
        SysUser u=new SysUser();u.setUserId(uid);u.setDeptId(school);u.setUserName(name);u.setNickName(nick);u.setStatus("0");u.setDelFlag("0");u.setPassword(SecurityUtils.encryptPassword(password));u.setCreateBy("bootstrap");u.setRoleIds(new Long[]{role});u.setPhonenumber(phone);users.insertUser(u);
        db.insert(BizTable.SECURITY,new LinkedHashMap<>(Map.of("user_id",uid,"password_version",1,"must_change_pwd",must?1:0,"phone_verified",phone==null?0:1)),"bootstrap");
        if(phone!=null)db.verifyPhone(uid,phone);
    }
}
