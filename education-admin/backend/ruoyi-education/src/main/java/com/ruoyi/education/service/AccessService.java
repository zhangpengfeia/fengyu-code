package com.ruoyi.education.service;

import com.ruoyi.common.core.domain.entity.*;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service
public class AccessService {
    final BizMapper db;
    final ISysUserService users;
    final SysPermissionService permissions;
    public AccessService(BizMapper db,ISysUserService users,SysPermissionService permissions) {
        this.db=db; this.users=users; this.permissions=permissions;
    }
    public static long id(Object v) { return v==null?0:Long.parseLong(v.toString()); }
    public static String text(Map<String,Object> m,String k) { return Objects.toString(m.get(k),""); }
    public static void deny() { throw new ServiceException("没有权限访问该资源",403); }
    public static Instant instant(Object v) {
        if(v instanceof java.util.Date x) return x.toInstant();
        if(v instanceof LocalDateTime x) return x.toInstant(ZoneOffset.UTC);
        return Instant.parse(v.toString());
    }
    public SysUser current() { return active(SecurityUtils.getUserId()); }
    public SysUser user(long uid) { return users.selectUserById(uid); }
    public void resetPassword(SysUser u) { users.resetPwd(u); }
    public SysUser active(long uid) {
        SysUser u=users.selectUserById(uid);
        if(u==null || !"0".equals(u.getDelFlag()) || !"0".equals(u.getStatus())) deny();
        if(!u.isAdmin()) {
            if(u.getDeptId()==null||db.disabledSchoolPath(u.getDeptId())>0) deny();
            if(hasSchoolIdentity(u)) schoolActive(u.getDeptId());
        }
        return u;
    }
    public boolean hasRole(SysUser u,String role) {
        return u.getRoles().stream().anyMatch(r->"0".equals(r.getStatus()) && role.equals(r.getRoleKey()));
    }
    public boolean student(SysUser u) { return hasRole(u,"edu_student"); }
    public boolean teacher(SysUser u) { return hasRole(u,"edu_teacher"); }
    public boolean hasSchoolIdentity(SysUser u) { return student(u)||teacher(u)||hasRole(u,"edu_school"); }
    public void schoolActive(long schoolId) {
        Map<String,Object> s=db.one(BizTable.SCHOOL,schoolId);
        if(s==null || !"ACTIVE".equals(s.get("school_status")) || !instant(s.get("expire_time")).isAfter(Instant.now())) deny();
        // Disabled administrative ancestors also deny access.
        // Department status is checked by the dedicated mapper to avoid cached user data.
        if(db.disabledSchoolPath(schoolId)>0) deny();
    }
    public void require(String permission) {
        SysUser u=current();
        if(!permits(u,permission)) deny();
    }
    public boolean permits(SysUser u,String permission) {var live=permissions.getMenuPermission(u);return u.isAdmin()||live.contains("*:*:*")||live.contains(permission);}
    public List<Long> schoolIds(SysUser u) {
        List<Long> all=db.schools(); if(u.isAdmin()) return all;
        Set<Long> allowed=new LinkedHashSet<>();
        for(SysRole r:u.getRoles()) {
            if(!"0".equals(r.getStatus())) continue;
            switch(Objects.toString(r.getDataScope(),"5")) {
                case "1" -> allowed.addAll(all);
                case "2" -> { for(Long d:db.roleDepts(r.getRoleId())) allowed.addAll(db.descendants(d)); }
                case "3" -> allowed.add(u.getDeptId());
                case "4" -> allowed.addAll(db.descendants(u.getDeptId()));
                default -> { if(hasSchoolIdentity(u)) allowed.add(u.getDeptId()); }
            }
        }
        return all.stream().filter(allowed::contains).toList();
    }
    public void school(long schoolId) { var actor=current();if(!actor.isAdmin()&&!schoolIds(actor).contains(schoolId)) deny(); }
    public boolean matches(Map<String,Object> scope,Map<String,Object> c,String idKey) {
        if(scope.get("game_id")!=null && !"game_id".equals(idKey)) return false;
        if(scope.get("courseware_id")!=null && !"courseware_id".equals(idKey)) return false;
        if(scope.get(idKey)!=null && id(scope.get(idKey))!=id(c.get(idKey))) return false;
        for(String k:List.of("subject_code","volume","stage_code")) {
            String value=text(scope,k);
            if(!value.isEmpty() && !"ANY".equals(value) && !value.equals(text(c,k))) return false;
        }
        return true;
    }
    public boolean courseware(SysUser u,Map<String,Object> c) {
        if(c==null||!"PUBLISHED".equals(c.get("status"))) return false;
        if(u.isAdmin()) return true;
        if(c.get("school_id")!=null && id(c.get("school_id"))!=u.getDeptId()) return false;
        if(!hasSchoolIdentity(u)) return schoolIds(u).contains(id(c.get("school_id")));
        schoolActive(u.getDeptId());
        boolean pool=db.pool(u.getDeptId()).stream().anyMatch(s->matches(s,c,"courseware_id"));
        if(!pool) return false;
        return !student(u)||db.scopes(u.getUserId()).stream().anyMatch(s->matches(s,c,"courseware_id"));
    }
    public boolean game(SysUser u,Map<String,Object> g) {
        if(g==null||!"PUBLISHED".equals(g.get("status"))) return false;
        if(u.isAdmin()) return true;
        if(!hasSchoolIdentity(u)) return false;
        schoolActive(u.getDeptId());
        String policy=text(g,"access_policy");
        if("TEACHER_ONLY".equals(policy)) return teacher(u)||hasRole(u,"edu_school");
        if("STUDENT".equals(policy)) return !student(u)||db.scopes(u.getUserId()).stream().anyMatch(s->
            s.get("game_id")!=null && id(s.get("game_id"))==id(g.get("game_id")));
        if("COURSEWARE".equals(policy)) return db.linkedCourseware(id(g.get("game_id"))).stream().anyMatch(c->courseware(u,c));
        return false;
    }
    public void userTarget(SysUser target) {
        SysUser actor=current();
        if(target==null) throw new ServiceException("用户不存在",404);
        if(!actor.isAdmin() && (target.isAdmin() || !schoolIds(actor).contains(target.getDeptId()) ||
            target.getRoles().stream().anyMatch(r->!List.of("edu_student","edu_teacher").contains(r.getRoleKey())))) deny();
    }
}
