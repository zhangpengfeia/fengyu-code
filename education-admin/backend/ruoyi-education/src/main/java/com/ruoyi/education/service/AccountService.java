package com.ruoyi.education.service;

import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@Service
public class AccountService {
    final BizMapper db;final AccessService access;final ISysUserService users;
    public AccountService(BizMapper db,AccessService access,ISysUserService users) {this.db=db;this.access=access;this.users=users;}
    public static String temporaryPassword() {
        byte[] b=new byte[15];new SecureRandom().nextBytes(b);
        return "Aa7!"+Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
    public static void password(String p) {
        if(p==null||p.length()<8||p.length()>32||
            ((p.matches(".*[A-Za-z].*")?1:0)+(p.matches(".*[0-9].*")?1:0)+(p.matches(".*[^A-Za-z0-9].*")?1:0))<2)
            throw new ServiceException("密码须为8至32位并包含至少两类字符",400);
    }
    public Map<String,Object> list(int page,int size,String keyword,String roleKey,String schoolId,String classId) {
        access.require("biz:user:list");var actor=access.current();Map<String,Object> q=new LinkedHashMap<>();
        if(!actor.isAdmin())q.put("schools",access.schoolIds(actor));
        if(access.teacher(actor))q.put("teacher",actor.getUserId());
        if(schoolId!=null&&!schoolId.isBlank())q.put("school",id(schoolId));
        if(classId!=null&&!classId.isBlank())q.put("class",id(classId));
        if(roleKey!=null&&!roleKey.isBlank())q.put("role",roleKey);
        if(keyword!=null&&!keyword.isBlank())q.put("keyword",keyword);
        int limit=Math.max(1,Math.min(size,100));q.put("limit",limit);q.put("offset",(Math.max(1,Math.min(page,100000))-1)*limit);
        var result=db.scopedUsers(q);
        for(var row:result) {
            SysUser u=users.selectUserById(id(row.get("user_id")));
            row.put("role_keys",u.getRoles().stream().filter(r->"0".equals(r.getStatus())).map(SysRole::getRoleKey).toList());
            row.put("can_edit",!u.getRoles().isEmpty()&&u.getRoles().stream().allMatch(r->List.of("edu_student","edu_teacher").contains(r.getRoleKey())||(actor.isAdmin()&&"edu_school".equals(r.getRoleKey()))));
            String phone=text(row,"phonenumber");if(phone.length()==11)row.put("phonenumber",phone.substring(0,3)+"****"+phone.substring(7));
        }
        return Map.of("code",200,"rows",result,"total",db.countUsers(q));
    }

    public Map<String,Object> detail(long uid) {
        access.require("biz:user:query");var target=users.selectUserById(uid);
        if(access.teacher(access.current())) {var s=db.one(BizTable.STUDENT,uid);if(s==null||db.teaches(access.current().getUserId(),id(s.get("class_id")))==0) deny();}
        else {
            var actor=access.current();if(target==null)throw new ServiceException("用户不存在",404);
            if(!actor.isAdmin()&&!access.schoolIds(actor).contains(target.getDeptId()))deny();
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("user_id",uid);out.put("user_name",target.getUserName());out.put("nick_name",target.getNickName());
        String phone=Objects.toString(target.getPhonenumber(),"");
        out.put("phone",phone.length()==11?phone.substring(0,3)+"****"+phone.substring(7):phone);
        out.put("school_id",target.getDeptId());out.put("status",target.getStatus());
        out.put("role_keys",target.getRoles().stream().map(SysRole::getRoleKey).toList());
        var student=db.one(BizTable.STUDENT,uid);if(student!=null) out.putAll(student);
        out.put("teaching",db.list(new BizQuery(BizTable.TEACHER).eq("teacher_user_id",uid)));return out;
    }
    @Transactional
    public Map<String,Object> save(Long uid,Map<String,Object> v) {
        access.require(uid==null?"biz:user:add":"biz:user:edit");SysUser actor=access.current();
        SysUser target=uid==null?new SysUser():users.selectUserById(uid);if(uid!=null) access.userTarget(target);
        long school=id(v.get("school_id"));access.school(school);access.schoolActive(school);
        String role=CatalogService.str(v,"role_key",32);
        if(!List.of("edu_student","edu_teacher","edu_school").contains(role)||(!actor.isAdmin()&&"edu_school".equals(role))) deny();
        if(uid!=null && target.getRoles().stream().noneMatch(r->role.equals(r.getRoleKey()))) throw new ServiceException("现有账号身份不可直接切换，请单独建立账号",400);
        String name=CatalogService.str(v,"nick_name",30);
        if(uid!=null && target.getDeptId()!=school) throw new ServiceException("转校请使用归属迁移流程，本期不开放直接修改学校",400);
        target.setNickName(name);target.setDeptId(school);target.setStatus(Objects.toString(v.get("status"),"0"));
        if(!List.of("0","1").contains(target.getStatus())) throw new ServiceException("状态无效",400);
        if(uid!=null&&uid==actor.getUserId()&&"1".equals(target.getStatus())) throw new ServiceException("不能停用当前账号",400);
        String phone=Objects.toString(v.get("phone"),"").trim();
        boolean changePhone=!phone.contains("*")&&!phone.equals(Objects.toString(target.getPhonenumber(),""));
        if(changePhone) {if(!phone.isEmpty()&&!phone.matches("1[3-9]\\d{9}")) throw new ServiceException("手机号格式无效",400);target.setPhonenumber(phone.isEmpty()?null:phone);}
        String temp=null;
        if(uid==null) {
            String username="edu_student".equals(role)?"s_"+UUID.randomUUID().toString().replace("-","").substring(0,20):CatalogService.str(v,"user_name",30);
            if(!username.matches("[A-Za-z][A-Za-z0-9_-]{2,29}")) throw new ServiceException("账号须以字母开头，长度3至30",400);
            target.setUserName(username);temp=temporaryPassword();target.setPassword(SecurityUtils.encryptPassword(temp));target.setCreateBy(actor.getUserName());
            target.setRoleIds(new Long[]{switch(role){case "edu_teacher"->102L;case "edu_student"->103L;default->101L;}});
            users.insertUser(target);uid=target.getUserId();
            Map<String,Object> security=new LinkedHashMap<>(Map.of("user_id",uid,"password_version",1,"must_change_pwd",1,"phone_verified",0));db.insert(BizTable.SECURITY,security,actor.getUserName());
        } else {target.setUpdateBy(actor.getUserName());target.setRoleIds(null);target.setPostIds(null);users.updateUserProfile(target);}
        if(changePhone) {db.phone(uid,phone.isEmpty()?null:phone);db.update(BizTable.SECURITY,uid,new LinkedHashMap<>(Map.of("phone_verified",0)),actor.getUserName());}
        if("edu_student".equals(role)) {
            long cid=id(v.get("class_id"));var cls=db.one(BizTable.CLASS,cid);
            if(cls==null||id(cls.get("school_id"))!=school||!"ACTIVE".equals(cls.get("status"))) throw new ServiceException("班级不存在、已归档或不属于该学校",400);
            String no=CatalogService.str(v,"student_no",32);var old=db.one(BizTable.STUDENT,uid);
            var student=new LinkedHashMap<String,Object>(Map.of("user_id",uid,"school_id",school,"class_id",cid,"student_no",no,"is_trial",0));
            if(old==null) db.insert(BizTable.STUDENT,student,actor.getUserName());else db.update(BizTable.STUDENT,uid,student,actor.getUserName());
            if(old==null||id(old.get("class_id"))!=cid) {
                db.closeHistory(uid);var history=new LinkedHashMap<String,Object>(Map.of("student_user_id",uid,"school_id",school,"class_id",cid,"start_time",new Date(),"change_reason",old==null?"入班":"转班"));
                db.insert(BizTable.HISTORY,history,actor.getUserName());
            }
        }
        if("edu_teacher".equals(role)) {
            Object vals=v.get("class_ids");List<?> cids=vals instanceof List<?> l?l:List.of();if(cids.size()>100) throw new ServiceException("任教班级过多",400);
            db.closeTeaching(uid);
            for(Object cid:new LinkedHashSet<>(cids)) {
                var cls=db.one(BizTable.CLASS,id(cid));if(cls==null||id(cls.get("school_id"))!=school||!"ACTIVE".equals(cls.get("status"))) throw new ServiceException("任教班级无效",400);
                db.insert(BizTable.TEACHER,new LinkedHashMap<>(Map.of("teacher_user_id",uid,"school_id",school,"class_id",id(cid),"subject_code","ANY","start_time",new Date())),actor.getUserName());
            }
        }
        return temp==null?Map.of("user_id",uid):Map.of("user_id",uid,"temporary_password",temp,"user_name",target.getUserName());
    }
    @Transactional
    public String reset(long uid) {
        access.require("biz:user:resetPwd");var u=users.selectUserById(uid);access.userTarget(u);
        String p=temporaryPassword();u.setPassword(SecurityUtils.encryptPassword(p));users.resetPwd(u);db.bumpPassword(uid,true);return p;
    }
}
