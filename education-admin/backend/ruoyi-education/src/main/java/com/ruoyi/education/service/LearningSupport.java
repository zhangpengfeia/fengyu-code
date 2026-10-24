package com.ruoyi.education.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

/** Shared school, teaching-class and student checks for every learning operation. */
@Service
public class LearningSupport {
    final BizMapper db; final AccessService access; final JdbcTemplate jdbc; final ObjectMapper json; final ISysConfigService config;
    public LearningSupport(BizMapper db, AccessService access, JdbcTemplate jdbc, ObjectMapper json, ISysConfigService config) {
        this.db=db; this.access=access; this.jdbc=jdbc; this.json=json; this.config=config;
    }
    public Map<String,Object> classroom(long cid, boolean write) {
        var c=db.one(BizTable.CLASS,cid); if(c==null) throw new ServiceException("班级不存在",404);
        var u=access.current();access.school(id(c.get("school_id")));
        if(access.teacher(u)&&db.teaches(u.getUserId(),cid)==0) deny();
        if(access.student(u)) {
            var s=db.one(BizTable.STUDENT,u.getUserId());
            if(write||s==null||id(s.get("class_id"))!=cid) deny();
        }
        if(write) {access.schoolActive(id(c.get("school_id")));if(!"ACTIVE".equals(c.get("status"))) throw new ServiceException("班级已归档",400);}
        return c;
    }
    public Map<String,Object> student(long uid,boolean write) {
        var s=db.one(BizTable.STUDENT,uid);if(s==null) throw new ServiceException("学生不存在",404);
        var u=access.current();if(access.student(u)&&u.getUserId()!=uid) deny();
        classroom(id(s.get("class_id")),write);return s;
    }
    public Map<String,Object> course(long cid,long course) {
        classroom(cid,true);var c=db.one(BizTable.COURSEWARE,course);
        if(c==null||!access.courseware(access.current(),c)) deny();return c;
    }
    public int number(String key,int fallback,int min,int max) {
        String v=config.selectConfigByKey(key);int n=v==null||v.isBlank()?fallback:Integer.parseInt(v);
        if(n<min||n>max)throw new ServiceException("系统参数范围无效："+key,400);return n;
    }
    public String encode(Object v) {try{return json.writeValueAsString(v);}catch(Exception e){throw new ServiceException("JSON数据无效",400);}}
    public Map<String,Object> object(Object v) {
        try {return json.readValue(v.toString(),new TypeReference<LinkedHashMap<String,Object>>(){});}
        catch(Exception e){throw new ServiceException("必须填写有效JSON对象",400);}
    }
    public long insert(BizTable t,Map<String,Object> values) {
        var v=new LinkedHashMap<>(values);db.insert(t,v,access.current().getUserName());return id(v.get("generatedId"));
    }
    public static int integer(Map<String,Object> v,String k,int min,int max) {
        try {long n=id(v.get(k));if(n<min||n>max)throw new IllegalArgumentException();return Math.toIntExact(n);}
        catch(Exception e){throw new ServiceException(k+"须在"+min+"至"+max+"之间",400);}
    }
    public static void url(Map<String,Object> v,String k,boolean required) {
        String value=Objects.toString(v.get(k),"").trim();
        if(value.isEmpty()&&!required)return;
        if(value.length()>1024||!(value.startsWith("https://")||value.startsWith("/profile/")))throw new ServiceException(k+"须为HTTPS或已上传文件路径",400);
        v.put(k,value);
    }
}
