package com.ruoyi.education.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;
import static com.ruoyi.education.service.LearningSupport.*;

@Service
public class DubbingService {
    final LearningSupport s;
    @org.springframework.beans.factory.annotation.Autowired(required=false)
    List<DubbingScorer> scorers=List.of();
    public DubbingService(LearningSupport s){this.s=s;}
    public List<Map<String,Object>> notices(){return s.jdbc.queryForList("SELECT n.notice_id,CONCAT(b.school_name,' 将在 ',e.days_before,' 天后到期') AS notice_title,CAST(n.notice_content AS CHAR) AS notice_content,n.create_time FROM sys_notice n JOIN biz_notice_recipient r ON r.notice_id=n.notice_id JOIN biz_school_expiry_notice e ON e.notice_id=n.notice_id JOIN biz_school b ON b.school_id=e.school_id WHERE r.user_id=? AND r.del_flag='0' AND n.status='0' ORDER BY n.notice_id DESC LIMIT 20",s.access.current().getUserId());}
    static BizTable table(String kind) {return switch(kind){case "materials"->BizTable.MATERIAL;case "exam-config"->BizTable.EXAM;default->throw new ServiceException("模块不存在",404);};}
    public Map<String,Object> list(String kind,int page,int size,Map<String,String> filters) {
        var t=table(kind);s.access.require("biz:dubbing:list");var q=new BizQuery(t);q.keyword=filters.get("keyword");
        for(String k:List.of("volume","status","material_tag"))if(t.writable.contains(k)&&filters.containsKey(k)&&!filters.get(k).isBlank())q.eq(k,filters.get(k));
        if(s.access.student(s.access.current())) {
            var student=s.student(s.access.current().getUserId(),false);
            q.eq("volume",student.get("current_volume")).eq("status","0");
        }
        q.limit=Math.max(1,Math.min(size,100));q.offset=(Math.max(1,Math.min(page,100000))-1)*q.limit;
        return Map.of("code",200,"rows",s.db.list(q),"total",s.db.count(q));
    }
    @Transactional public long save(String kind,Long rid,Map<String,Object> payload) {
        s.access.require("biz:dubbing:edit");var t=table(kind);
        if(!s.access.current().isAdmin())deny();
        var v=new LinkedHashMap<String,Object>();for(String k:t.writable)if(payload.containsKey(k))v.put(k,payload.get(k));
        integer(v,"volume",1,12);CatalogService.choice(v,"status","0","1");
        if(t==BizTable.MATERIAL) {
            CatalogService.str(v,"title",128);CatalogService.str(v,"material_text",20000);url(v,"video_url",true);
            var tags=new LinkedHashSet<>(Arrays.asList(text(v,"material_tag").split(",")));
            if(tags.isEmpty()||!Set.of("01","02","03").containsAll(tags))throw new ServiceException("用途标签无效",400);
            v.put("material_tag",String.join(",",tags));
            if(rid!=null) {
                var refs=s.jdbc.queryForList("SELECT c.volume FROM biz_courseware_read r JOIN biz_courseware_base c ON r.courseware_id=c.courseware_id WHERE r.dubbing_material_id=? AND c.del_flag='0'",rid);
                if(!refs.isEmpty()&&(!"0".equals(v.get("status"))||!courseTag(v)||refs.stream().anyMatch(r->id(r.get("volume"))!=id(v.get("volume")))))throw new ServiceException("素材已关联课件，不能停用、移册或移除课件用途",400);
            }
        } else {
            CatalogService.choice(v,"extract_scope","1","2","3");CatalogService.choice(v,"extract_mode","1","2");
            CatalogService.choice(v,"score_rule","1","2");integer(v,"extract_num",1,100);
            if("2".equals(v.get("score_rule")))throw new ServiceException("自定义权重需配置评分供应商后启用",400);
        }
        if(rid==null)return s.insert(t,v);
        var old=s.db.one(t,rid);if(old==null)throw new ServiceException("记录不存在",404);
        if(!payload.containsKey("row_version"))throw new ServiceException("请刷新后重试",409);
        v.put("expected_row_version",payload.get("row_version"));
        if(s.db.update(t,rid,v,s.access.current().getUserName())!=1)throw new ServiceException("记录已更新，请刷新",409);return rid;
    }
    static boolean courseTag(Map<String,Object> v){var tags=Arrays.asList(text(v,"material_tag").split(","));return tags.contains("01")||tags.contains("03");}
    @Transactional public void remove(long mid) {
        s.access.require("biz:dubbing:edit");if(!s.access.current().isAdmin())deny();
        if(!s.jdbc.queryForList("SELECT courseware_id FROM biz_courseware_read WHERE dubbing_material_id=? AND del_flag='0'",mid).isEmpty())throw new ServiceException("素材已关联课件",400);
        s.db.delete(BizTable.MATERIAL,mid,s.access.current().getUserName());
    }
    public void validateMaterial(long mid,int volume) {
        var m=s.db.one(BizTable.MATERIAL,mid);
        if(m==null||id(m.get("volume"))!=volume||!"0".equals(m.get("status"))||!courseTag(m))throw new ServiceException("请选择本册且启用的课件配套或通用素材",400);
    }
    @Transactional public List<Map<String,Object>> draw(int volume) {
        s.access.require("biz:dubbing:list");var u=s.access.current();
        if(!s.access.student(u))deny();var student=s.student(u.getUserId(),false);
        if(id(student.get("current_volume"))!=volume)deny();s.access.schoolActive(id(student.get("school_id")));
        var configs=s.db.list(new BizQuery(BizTable.EXAM).eq("volume",volume).eq("status","0"));
        if(configs.isEmpty())throw new ServiceException("本册测评未启用",400);var cfg=configs.get(0);
        var q=new BizQuery(BizTable.MATERIAL).eq("volume",volume).eq("status","0");q.limit=10000;
        var materials=new ArrayList<>(s.db.list(q));String scope=text(cfg,"extract_scope");
        materials.removeIf(m->!"3".equals(scope)&&!Arrays.asList(text(m,"material_tag").split(",")).contains("03")&&!Arrays.asList(text(m,"material_tag").split(",")).contains("1".equals(scope)?"01":"02"));
        if("2".equals(cfg.get("extract_mode"))) {
            var done=s.jdbc.queryForList("SELECT DISTINCT material_id FROM biz_student_dubbing_record WHERE student_id=? AND volume=? AND dubbing_type='2' AND del_flag='0'",u.getUserId(),volume);
            var ids=done.stream().map(r->id(r.get("material_id"))).toList();materials.removeIf(m->ids.contains(id(m.get("material_id"))));
        }
        int n=(int)id(cfg.get("extract_num"));if(materials.size()<n)throw new ServiceException("本册符合条件的未完成素材数量不足",400);
        Collections.shuffle(materials,new java.security.SecureRandom());var selected=materials.subList(0,n);
        for(var material:selected) {
            String ticket=UUID.randomUUID().toString();s.jdbc.update("INSERT INTO biz_dubbing_exam_ticket(ticket_id,student_id,material_id,expires_at,create_by) VALUES(?,?,?,?,?)",ticket,u.getUserId(),material.get("material_id"),java.sql.Timestamp.from(java.time.Instant.now().plusSeconds(1800)),u.getUserName());material.put("exam_ticket",ticket);
        }
        return selected;
    }
    public Map<String,Object> records(int page,int size,Long cid,Long uid,Integer volume,String type) {
        s.access.require("biz:dubbing:list");var u=s.access.current();List<Object> args=new ArrayList<>();String where=" WHERE r.del_flag='0'";
        if(s.access.student(u)){uid=u.getUserId();}
        if(uid!=null){s.student(uid,false);where+=" AND r.student_id=?";args.add(uid);}
        if(cid!=null){s.classroom(cid,false);where+=" AND r.class_id=?";args.add(cid);}
        if(!u.isAdmin()) {
            var schools=s.access.schoolIds(u);where+=" AND r.school_id IN ("+(schools.isEmpty()?"NULL":String.join(",",Collections.nCopies(schools.size(),"?")))+")";args.addAll(schools);
            if(s.access.teacher(u)){where+=" AND EXISTS(SELECT 1 FROM biz_teacher_class tc WHERE tc.class_id=r.class_id AND tc.teacher_user_id=? AND tc.del_flag='0' AND tc.start_time<=CURRENT_TIMESTAMP AND (tc.end_time IS NULL OR tc.end_time>CURRENT_TIMESTAMP))";args.add(u.getUserId());}
        }
        if(volume!=null){where+=" AND r.volume=?";args.add(volume);}if(type!=null&&!type.isBlank()){where+=" AND r.dubbing_type=?";args.add(type);}
        long total=s.jdbc.queryForObject("SELECT COUNT(*) FROM biz_student_dubbing_record r"+where,Long.class,args.toArray());
        int limit=Math.max(1,Math.min(size,100));args.add(limit);args.add((Math.max(1,Math.min(page,100000))-1)*limit);
        var rows=s.jdbc.queryForList("SELECT r.*,m.title FROM biz_student_dubbing_record r JOIN biz_dubbing_material m ON r.material_id=m.material_id"+where+" ORDER BY r.answer_time DESC,r.record_id DESC LIMIT ? OFFSET ?",args.toArray());
        return Map.of("code",200,"rows",rows,"total",total);
    }
    public void requireExportPermission(){s.access.require("biz:dubbing:export");}
    @Transactional public Map<String,Object> score(Map<String,Object> payload) {
        // Never accept client supplied marks as trusted AI results.
        s.access.require("biz:dubbing:list");var u=s.access.current();if(!s.access.student(u))deny();
        var student=s.student(u.getUserId(),false);s.access.schoolActive(id(student.get("school_id")));
        var m=s.db.one(BizTable.MATERIAL,id(payload.get("material_id")));
        if(m==null||id(m.get("volume"))!=id(student.get("current_volume"))||!"0".equals(m.get("status")))deny();
        if(scorers.size()!=1)throw new ServiceException("尚未配置百度AI评分适配器，无法生成真实评分",503);
        CatalogService.choice(payload,"dubbing_type","1","2");url(payload,"audio_url",true);
        String type=text(payload,"dubbing_type"),ticket=text(payload,"exam_ticket");Long course=null;
        if("1".equals(type)) {
            course=id(payload.get("courseware_id"));var c=s.db.one(BizTable.COURSEWARE,course);var ext=s.db.one(BizTable.READ,course);
            if(c==null||ext==null||!s.access.courseware(u,c)||id(ext.get("dubbing_material_id"))!=id(m.get("material_id")))deny();
        } else {
            var tickets=s.jdbc.queryForList("SELECT * FROM biz_dubbing_exam_ticket WHERE ticket_id=? AND student_id=? AND material_id=? AND del_flag='0' FOR UPDATE",ticket,u.getUserId(),m.get("material_id"));
            if(tickets.isEmpty())throw new ServiceException("请先抽取测评素材",400);var issued=tickets.get(0);
            if(issued.get("used_record_id")!=null)return Map.of("record_id",issued.get("used_record_id"));
            if(!instant(issued.get("expires_at")).isAfter(java.time.Instant.now()))throw new ServiceException("测评已过期，请重新抽题",400);
        }
        DubbingScorer.Result result;
        try {result=scorers.get(0).evaluate(text(m,"material_text"),text(payload,"audio_url"));}
        catch(Exception e){org.slf4j.LoggerFactory.getLogger(DubbingService.class).error("配音评分供应商异常 material={}",m.get("material_id"),e);throw new ServiceException("AI评分服务异常，请稍后重试",503);}
        if(result==null||result.accuracy()<0||result.accuracy()>20||result.fluency()<0||result.fluency()>20||result.completeness()<0||result.completeness()>15||result.phonology()<0||result.phonology()>15||result.intonation()<0||result.intonation()>15||result.overall()<0||result.overall()>15)throw new ServiceException("评分供应商返回分值无效",503);
        var cls=s.db.one(BizTable.CLASS,id(student.get("class_id")));var record=new LinkedHashMap<String,Object>();
        record.put("school_id",student.get("school_id"));record.put("grade",cls.get("grade"));record.put("class_id",student.get("class_id"));record.put("student_id",u.getUserId());record.put("volume",m.get("volume"));record.put("material_id",m.get("material_id"));record.put("courseware_id",course);record.put("dubbing_type",type);record.put("answer_time",new Date());record.put("audio_url",payload.get("audio_url"));
        record.put("accuracy_score",result.accuracy());record.put("fluency_score",result.fluency());record.put("completeness_score",result.completeness());record.put("phonology_score",result.phonology());record.put("intonation_score",result.intonation());record.put("overall_score",result.overall());record.put("total_score",result.total());
        if("2".equals(type)) {
            String configured=s.config.selectConfigByKey("biz.dubbing.starThresholds");String[] thresholds=(configured==null||configured.isBlank()?"60,75,90":configured).split(",");
            int stars=0,previous=-1;for(String threshold:thresholds){int value=Integer.parseInt(threshold.trim());if(value<=previous||value>100)throw new ServiceException("得星门槛参数无效",400);previous=value;if(result.total()>=value)stars++;}record.put("star_num",stars);
        }
        long rid=s.insert(BizTable.RECORD,record);
        if("2".equals(type))s.jdbc.update("UPDATE biz_dubbing_exam_ticket SET used_record_id=?,update_time=CURRENT_TIMESTAMP WHERE ticket_id=?",rid,ticket);
        return Map.of("record_id",rid,"total_score",result.total(),"star_num",record.getOrDefault("star_num",0));
    }
}
