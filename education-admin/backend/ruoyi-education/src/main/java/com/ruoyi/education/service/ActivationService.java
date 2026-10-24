package com.ruoyi.education.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;
import static com.ruoyi.education.service.LearningSupport.*;

@Service
public class ActivationService {
    final LearningSupport s;final SecureRandom random=new SecureRandom();
    public ActivationService(LearningSupport s){this.s=s;}
    public Map<String,Object> batches(int page,int size) {
        s.access.require("biz:activation:list");if(!s.access.current().isAdmin())deny();
        var q=new BizQuery(BizTable.BATCH);q.limit=Math.max(1,Math.min(size,100));q.offset=(Math.max(1,Math.min(page,100000))-1)*q.limit;
        return Map.of("code",200,"rows",s.db.list(q),"total",s.db.count(q));
    }
    @Transactional public long generate(Map<String,Object> payload) {
        s.access.require("biz:activation:edit");if(!s.access.current().isAdmin())deny();
        var v=new LinkedHashMap<String,Object>();for(String k:BizTable.BATCH.writable)if(payload.containsKey(k))v.put(k,payload.get(k));v.remove("used_num");
        CatalogService.str(v,"batch_name",128);CatalogService.choice(v,"permission_type","1","2","3");
        int days=integer(v,"valid_days",1,3650),n=integer(v,"total_num",1,10000);v.put("valid_days",days);
        CatalogService.date(v,"expire_time");if(!instant(v.get("expire_time")).isAfter(Instant.now()))throw new ServiceException("批次有效期须晚于当前时间",400);
        Map<String,Object> range=s.object(v.get("permission_range"));
        if("2".equals(v.get("permission_type")))validateRange(range);
        v.put("permission_range",s.encode(range));
        if("true".equals(s.config.selectConfigByKey("biz.activation.schoolBound"))) {long school=id(v.get("school_id"));s.access.schoolActive(school);}
        else if(v.get("school_id")!=null)throw new ServiceException("请先开启学校绑定参数",400);
        long batch=s.insert(BizTable.BATCH,v);
        for(int i=0;i<n;i++) {byte[] bytes=new byte[15];random.nextBytes(bytes);String code=HexFormat.of().withUpperCase().formatHex(bytes);s.insert(BizTable.CODE,new LinkedHashMap<>(Map.of("batch_id",batch,"code",code,"status","0")));}
        return batch;
    }
    public List<Map<String,Object>> codes(long batch) {
        s.access.require("biz:activation:export");if(!s.access.current().isAdmin())deny();
        var q=new BizQuery(BizTable.CODE).eq("batch_id",batch);q.limit=10000;return s.db.list(q);
    }
    static void validateRange(Map<String,Object> range) {
        if(!range.keySet().equals(Set.of("volumes","subjects","stages")))throw new ServiceException("范围须包含volumes、subjects、stages三个数组",400);
        for(String k:range.keySet())if(!(range.get(k) instanceof List<?> list)||list.isEmpty()||list.size()>12)throw new ServiceException("范围数组不能为空或超过12项",400);
        for(Object n:(List<?>)range.get("volumes"))if(id(n)<1||id(n)>12)throw new ServiceException("册次范围无效",400);
        if(!Set.of("MATH","READING","SCIENCE","CREATIVE").containsAll((List<?>)range.get("subjects")))throw new ServiceException("学科范围无效",400);
        if(!Set.of("BASIC","LEVEL1","LEVEL2","LEVEL3","ANY").containsAll((List<?>)range.get("stages")))throw new ServiceException("阶次范围无效",400);
    }
    @Transactional public long redeem(String code) {
        var u=s.access.current();if(!s.access.student(u))deny();var student=s.student(u.getUserId(),false);s.access.schoolActive(id(student.get("school_id")));
        if(code==null||!code.matches("[A-Za-z0-9]{16,64}"))throw new ServiceException("激活码无效",400);
        var rows=s.jdbc.queryForList("SELECT * FROM biz_activate_code WHERE code=? AND del_flag='0' FOR UPDATE",code.toUpperCase(Locale.ROOT));
        if(rows.isEmpty())throw new ServiceException("激活码不存在",400);var c=rows.get(0);
        var b=s.db.one(BizTable.BATCH,id(c.get("batch_id")));
        if(b==null||!"0".equals(c.get("status"))||!instant(b.get("expire_time")).isAfter(Instant.now()))throw new ServiceException("激活码已使用或已过期",400);
        if(b.get("school_id")!=null&&id(b.get("school_id"))!=id(student.get("school_id")))deny();
        var grant=new LinkedHashMap<String,Object>();grant.put("student_user_id",u.getUserId());grant.put("source_type","ACTIVATION");grant.put("source_id",c.get("code_id"));grant.put("status","ACTIVE");
        Instant now=Instant.now();grant.put("start_time",Timestamp.from(now));grant.put("end_time",Timestamp.from(now.plus(Duration.ofDays(id(b.get("valid_days"))))));
        long eid=s.insert(BizTable.GRANT,grant);String type=text(b,"permission_type");
        if("3".equals(type))s.db.scope(eid,Map.of("subject_code","ANY"));
        else if("1".equals(type))s.db.scope(eid,Map.of("volume",student.get("current_volume"),"subject_code","ANY"));
        else {
            var range=s.object(b.get("permission_range"));validateRange(range);
            for(Object volume:(List<?>)range.get("volumes"))for(Object subject:(List<?>)range.get("subjects"))for(Object stage:(List<?>)range.get("stages"))s.db.scope(eid,Map.of("volume",volume,"subject_code",subject,"stage_code",stage));
        }
        if(s.jdbc.update("UPDATE biz_activate_code SET status='1',student_id=?,use_time=CURRENT_TIMESTAMP,update_by=?,update_time=CURRENT_TIMESTAMP WHERE code_id=? AND status='0'",u.getUserId(),u.getUserName(),c.get("code_id"))!=1)throw new ServiceException("激活码已被兑换",409);
        s.jdbc.update("UPDATE biz_activate_code_batch SET used_num=used_num+1,update_time=CURRENT_TIMESTAMP WHERE batch_id=?",c.get("batch_id"));return eid;
    }
}
