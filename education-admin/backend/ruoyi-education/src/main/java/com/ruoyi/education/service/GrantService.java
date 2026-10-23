package com.ruoyi.education.service;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

@Service
public class GrantService {
    final BizMapper db;final AccessService access;
    public GrantService(BizMapper db,AccessService access) {this.db=db;this.access=access;}
    public List<Map<String,Object>> list(long student) {
        if(access.current().getUserId()!=student) {access.require("biz:entitlement:list");access.userTarget(access.users.selectUserById(student));}
        var q=new BizQuery(BizTable.GRANT).eq("student_user_id",student);q.limit=1000;var rows=db.list(q);rows.forEach(r->r.put("scopes",db.grantScopes(id(r.get("entitlement_id")))));return rows;
    }
    @Transactional
    public long grant(Map<String,Object> payload) {
        access.require("biz:entitlement:grant");long student=id(payload.get("student_user_id"));
        var target=access.users.selectUserById(student);access.userTarget(target);
        if(!access.student(target)) throw new ServiceException("仅可为学生发放学习授权",400);
        Map<String,Object> v=new LinkedHashMap<>();
        v.put("student_user_id",student);v.put("start_time",payload.get("start_time"));v.put("end_time",payload.get("end_time"));
        CatalogService.date(v,"start_time");CatalogService.date(v,"end_time");
        if(!instant(v.get("end_time")).isAfter(instant(v.get("start_time")))) throw new ServiceException("到期时间须晚于开始时间",400);
        v.put("status","ACTIVE");v.put("source_type","MANUAL");
        String reason=Objects.toString(payload.get("remark"),"").trim();if(reason.isEmpty()||reason.length()>500) throw new ServiceException("必须填写授权原因，最多500字",400);v.put("remark",reason);
        String type=Objects.toString(payload.get("scope_type"),"COURSEWARE");Map<String,Object> scope=new LinkedHashMap<>();
        if("GAME".equals(type)) {
            long gid=id(payload.get("game_id"));var game=db.one(BizTable.GAME,gid);
            if(game==null||!"STUDENT".equals(game.get("access_policy"))) throw new ServiceException("独立授权仅支持允许学生的游戏",400);scope.put("game_id",gid);
        } else if("COURSEWARE".equals(type)) {
            long cid=id(payload.get("courseware_id"));var c=db.one(BizTable.COURSEWARE,cid);
            if(c==null||!"PUBLISHED".equals(c.get("status"))||(c.get("school_id")!=null&&id(c.get("school_id"))!=target.getDeptId())) throw new ServiceException("课件不在目标学生学校范围内",400);
            scope.put("courseware_id",cid);
        } else throw new ServiceException("第一期仅支持单课件或单游戏授权",400);
        db.insert(BizTable.GRANT,v,access.current().getUserName());long eid=id(v.get("generatedId"));db.scope(eid,scope);return eid;
    }
    @Transactional
    public void revoke(long eid,String reason) {
        access.require("biz:entitlement:grant");var old=db.one(BizTable.GRANT,eid);if(old==null) throw new ServiceException("授权不存在",404);
        access.userTarget(access.users.selectUserById(id(old.get("student_user_id"))));
        if(reason==null||reason.isBlank()||reason.length()>500) throw new ServiceException("请填写撤销原因",400);
        db.update(BizTable.GRANT,eid,new LinkedHashMap<>(Map.of("status","REVOKED","remark",reason)),access.current().getUserName());
    }
}
