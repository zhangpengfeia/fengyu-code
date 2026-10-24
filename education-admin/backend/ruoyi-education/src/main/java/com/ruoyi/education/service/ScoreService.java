package com.ruoyi.education.service;

import com.ruoyi.common.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;
import static com.ruoyi.education.service.LearningSupport.*;

@Service
public class ScoreService {
    final LearningSupport s;
    public ScoreService(LearningSupport s){this.s=s;}
    @Transactional public void adjust(Map<String,Object> p) {
        s.access.require("biz:score:edit");long uid=id(p.get("student_id"));var student=s.student(uid,true);
        long cid=id(student.get("class_id")),course=id(p.get("courseware_id"));var c=s.course(cid,course);
        String source=CatalogService.str(p,"source",64);String key=CatalogService.str(p,"event_key",100);
        int delta=integer(p,"score",-100,100);if(delta==0)throw new ServiceException("积分变更不能为0",400);
        post(uid,cid,c,text(p,"stage").isBlank()?"BASIC":text(p,"stage"),delta,source,"MANUAL:"+key,null);
    }
    /** Called in the caller's transaction. Lock the student row before ledger and aggregate changes. */
    void post(long uid,long cid,Map<String,Object> course,String stage,int delta,String source,String key,Long reversal) {
        if(!Set.of("BASIC","LEVEL1","LEVEL2","LEVEL3").contains(stage))throw new ServiceException("积分阶次无效",400);
        var rows=s.jdbc.queryForList("SELECT * FROM biz_student_extend WHERE user_id=? AND del_flag='0' FOR UPDATE",uid);
        if(rows.isEmpty())throw new ServiceException("学生不存在",404);var student=rows.get(0);
        if(id(student.get("class_id"))!=cid)throw new ServiceException("学生已转班，请刷新后重试",409);
        var cls=s.db.one(com.ruoyi.education.domain.BizTable.CLASS,cid);var u=s.access.current();
        s.jdbc.update("INSERT INTO biz_score_log(student_id,school_id,class_id,grade,courseware_id,subject,volume,stage,score,source,operator,event_key,reversal_of,create_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",uid,student.get("school_id"),cid,cls.get("grade"),course.get("courseware_id"),course.get("subject_code"),course.get("volume"),stage,delta,source,u.getUserId(),key,reversal,u.getUserName());
        Map<String,Object> volumes=student.get("volume_score")==null?new LinkedHashMap<>():s.object(student.get("volume_score"));
        Map<String,Object> grades=student.get("grade_score")==null?new LinkedHashMap<>():s.object(student.get("grade_score"));
        String volume=text(course,"volume"),grade=text(cls,"grade");volumes.put(volume,id(volumes.get(volume))+delta);grades.put(grade,id(grades.get(grade))+delta);
        s.jdbc.update("UPDATE biz_student_extend SET total_score=total_score+?,volume_score=?,grade_score=?,update_by=?,update_time=CURRENT_TIMESTAMP,row_version=row_version+1 WHERE user_id=?",delta,s.encode(volumes),s.encode(grades),u.getUserName(),uid);
    }
    @Transactional public void allPlus(long cid,long course) {
        s.access.require("biz:score:edit");s.classroom(cid,true);s.course(cid,course);
        for(long uid:s.jdbc.queryForList("SELECT st.user_id FROM biz_student_extend st JOIN sys_user u ON u.user_id=st.user_id WHERE st.class_id=? AND st.del_flag='0' AND u.status='0' AND u.del_flag='0' ORDER BY st.user_id",Long.class,cid)) {
            var p=new LinkedHashMap<String,Object>(Map.of("student_id",uid,"courseware_id",course,"score",1,"source","课堂表现全体+1","event_key",UUID.randomUUID().toString()));adjust(p);
        }
    }
    public List<Map<String,Object>> schoolRanking(long school,Integer grade,Integer volume) {
        s.access.require("biz:score:list");s.access.school(school);var actor=s.access.current();if(s.access.teacher(actor)||s.access.student(actor))deny();
        var args=new ArrayList<Object>();String filter="l.school_id=?";args.add(school);
        if(grade!=null){filter+=" AND l.grade=?";args.add(grade);}if(volume!=null){filter+=" AND l.volume=?";args.add(volume);}
        args.add(school);
        return s.jdbc.queryForList("SELECT st.user_id,u.nick_name,COALESCE(SUM(l.score),0) total_score FROM biz_student_extend st JOIN sys_user u ON u.user_id=st.user_id LEFT JOIN biz_score_log l ON l.student_id=st.user_id AND l.del_flag='0' AND "+filter+" WHERE st.school_id=? AND st.del_flag='0' AND st.is_trial=0 GROUP BY st.user_id,u.nick_name ORDER BY total_score DESC,st.user_id",args.toArray());
    }
    @Transactional public void reverse(long logId) {
        s.access.require("biz:score:edit");
        var rows=s.jdbc.queryForList("SELECT * FROM biz_score_log WHERE log_id=? AND del_flag='0' FOR UPDATE",logId);
        if(rows.isEmpty())throw new ServiceException("积分记录不存在",404);var log=rows.get(0);
        if(log.get("reversal_of")!=null||!text(log,"event_key").startsWith("MANUAL:"))throw new ServiceException("请通过原业务撤销该积分",400);
        long uid=id(log.get("student_id"));var st=s.student(uid,true);if(id(st.get("class_id"))!=id(log.get("class_id")))throw new ServiceException("历史班级积分需由原业务处理",400);
        post(uid,id(log.get("class_id")),s.db.one(com.ruoyi.education.domain.BizTable.COURSEWARE,id(log.get("courseware_id"))),text(log,"stage"),-(int)id(log.get("score")),"撤销积分","REVERSE:"+logId,logId);
    }
    public List<Map<String,Object>> ledger(long uid) {
        s.access.require("biz:score:list");s.student(uid,false);
        return s.jdbc.queryForList("SELECT * FROM biz_score_log WHERE student_id=? AND del_flag='0' ORDER BY log_id DESC LIMIT 1000",uid);
    }
    public List<Map<String,Object>> ranking(long cid,Long course,Integer volume) {
        s.access.require("biz:score:list");s.classroom(cid,false);
        var args=new ArrayList<Object>();String condition="l.class_id=?";args.add(cid);
        if(course!=null){condition+=" AND l.courseware_id=?";args.add(course);}if(volume!=null){condition+=" AND l.volume=?";args.add(volume);}
        return s.jdbc.queryForList("SELECT st.user_id,u.nick_name,COALESCE(SUM(l.score),0) total_score FROM biz_student_extend st JOIN sys_user u ON u.user_id=st.user_id LEFT JOIN biz_score_log l ON l.student_id=st.user_id AND l.del_flag='0' AND "+condition+" WHERE st.class_id=? AND st.del_flag='0' AND st.is_trial=0 GROUP BY st.user_id,u.nick_name ORDER BY total_score DESC,st.user_id",append(args,cid));
    }
    static Object[] append(List<Object> args,Object value){args.add(value);return args.toArray();}
}
