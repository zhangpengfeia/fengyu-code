package com.ruoyi.education.service;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.education.domain.*;
import com.ruoyi.framework.web.service.SysRegisterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@Service
public class StudentRegistrationService {
    final LearningSupport s;final SysRegisterService registration;final GroupingService grouping;
    public StudentRegistrationService(LearningSupport s,SysRegisterService registration,GroupingService grouping){this.s=s;this.registration=registration;this.grouping=grouping;}
    public List<Map<String,Object>> schools() {
        var rows=s.jdbc.queryForList("SELECT s.school_id,s.school_name,d.parent_id,d.ancestors FROM biz_school s JOIN sys_dept d ON d.dept_id=s.school_id WHERE s.del_flag='0' AND s.school_status='ACTIVE' AND s.expire_time>CURRENT_TIMESTAMP AND d.status='0' AND d.del_flag='0'");
        rows.removeIf(r->s.db.disabledSchoolPath(id(r.get("school_id")))>0);return rows;
    }
    public List<Map<String,Object>> tree() {
        var allowed=new HashSet<Long>();
        for(var school:schools()) {allowed.add(id(school.get("school_id")));for(String ancestor:text(school,"ancestors").split(","))if(!ancestor.isBlank())allowed.add(Long.parseLong(ancestor));}
        var departments=s.jdbc.queryForList("SELECT dept_id,parent_id,dept_name FROM sys_dept WHERE del_flag='0' AND status='0' ORDER BY order_num,dept_id");
        Map<Long,Map<String,Object>> nodes=new LinkedHashMap<>();
        for(var d:departments)if(allowed.contains(id(d.get("dept_id")))) {var node=new LinkedHashMap<String,Object>();node.put("value",d.get("dept_id").toString());node.put("label",d.get("dept_name"));nodes.put(id(d.get("dept_id")),node);}
        var roots=new ArrayList<Map<String,Object>>();
        for(var d:departments) {var node=nodes.get(id(d.get("dept_id")));if(node==null)continue;var parent=nodes.get(id(d.get("parent_id")));if(parent==null)roots.add(node);else { @SuppressWarnings("unchecked") var children=(List<Map<String,Object>>)parent.computeIfAbsent("children",k->new ArrayList<Map<String,Object>>());children.add(node);}}
        return roots;
    }
    public List<Map<String,Object>> classes(long school) {
        s.access.schoolActive(school);return s.jdbc.queryForList("SELECT class_id,class_name,grade FROM biz_class WHERE school_id=? AND status='ACTIVE' AND del_flag='0' ORDER BY grade,class_id",school);
    }
    @Transactional public boolean register(Map<String,Object> p) {
        String password=text(p,"password");AccountService.password(password);
        if(s.config.selectCaptchaEnabled())registration.validateCaptcha("student",text(p,"code"),text(p,"uuid"));
        long school=id(p.get("school_id")),cid=id(p.get("class_id"));s.access.schoolActive(school);
        var cls=s.db.one(BizTable.CLASS,cid);if(cls==null||id(cls.get("school_id"))!=school||!"ACTIVE".equals(cls.get("status")))throw new ServiceException("请选择该学校的有效班级",400);
        String no=CatalogService.str(p,"student_no",32),name=CatalogService.str(p,"nick_name",30),phone=text(p,"phone").trim();
        if(!phone.isEmpty()&&!phone.matches("1[3-9]\\d{9}"))throw new ServiceException("手机号格式无效",400);
        if(s.db.studentUser(school,no)!=null)throw new ServiceException("该学校学号已注册",409);
        boolean audit="true".equals(s.config.selectConfigByKey("biz.student.register.audit"));
        var u=new SysUser();u.setUserName("s_"+UUID.randomUUID().toString().replace("-","").substring(0,20));u.setNickName(name);u.setDeptId(school);u.setPhonenumber(phone.isEmpty()?null:phone);
        if(!s.access.users.checkPhoneUnique(u))throw new ServiceException("手机号已被占用",409);
        u.setPassword(SecurityUtils.encryptPassword(password));u.setStatus(audit?"1":"0");u.setDelFlag("0");u.setCreateBy("student-registration");u.setRoleIds(new Long[]{103L});s.access.users.insertUser(u);
        long uid=u.getUserId();s.db.insert(BizTable.SECURITY,new LinkedHashMap<>(Map.of("user_id",uid,"must_change_pwd",0,"phone_verified",0)),"student-registration");
        s.db.insert(BizTable.STUDENT,new LinkedHashMap<>(Map.of("user_id",uid,"school_id",school,"class_id",cid,"student_no",no,"current_volume",id(cls.get("grade"))*2-1)),"student-registration");
        grouping.join(uid,cid,"student-registration");
        s.db.insert(BizTable.HISTORY,new LinkedHashMap<>(Map.of("student_user_id",uid,"school_id",school,"class_id",cid,"start_time",new Date(),"change_reason","学生自主注册")),"student-registration");return audit;
    }
}
