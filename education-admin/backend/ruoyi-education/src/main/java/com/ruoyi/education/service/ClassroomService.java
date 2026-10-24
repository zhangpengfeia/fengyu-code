package com.ruoyi.education.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.education.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;
import static com.ruoyi.education.service.LearningSupport.*;

@Service
public class ClassroomService {
    final LearningSupport s;final ScoreService scores;
    public ClassroomService(LearningSupport s,ScoreService scores){this.s=s;this.scores=scores;}
    void lock(long cid){s.jdbc.queryForList("SELECT class_id FROM biz_class WHERE class_id=? FOR UPDATE",cid);}
    public List<Map<String,Object>> groups(long cid) {
        s.access.require("biz:pk:list");s.classroom(cid,false);
        var q=new BizQuery(BizTable.GROUP).eq("class_id",cid).eq("is_current","1");q.limit=10000;
        var groups=s.db.list(q);for(var g:groups)g.put("members",s.jdbc.queryForList("SELECT sg.student_id,u.nick_name FROM biz_student_group sg JOIN biz_student_extend st ON st.user_id=sg.student_id JOIN sys_user u ON u.user_id=sg.student_id WHERE sg.group_id=? AND sg.is_current='1' AND sg.del_flag='0' AND st.class_id=? AND st.del_flag='0' ORDER BY sg.student_id",g.get("group_id"),cid));return groups;
    }
    @Transactional public void regroup(long cid,int size) {
        s.access.require("biz:pk:edit");s.classroom(cid,true);if(size<2||size>6)throw new ServiceException("每组人数须为2至6",400);lock(cid);
        var ids=new ArrayList<>(s.jdbc.queryForList("SELECT st.user_id FROM biz_student_extend st JOIN sys_user u ON u.user_id=st.user_id WHERE st.class_id=? AND st.del_flag='0' AND u.status='0' AND u.del_flag='0' ORDER BY st.user_id",Long.class,cid));
        if(ids.isEmpty())throw new ServiceException("班级没有可分组学生",400);
        s.jdbc.update("UPDATE biz_student_group SET is_current='0',update_by=?,update_time=CURRENT_TIMESTAMP WHERE group_id IN (SELECT group_id FROM biz_class_group WHERE class_id=?) AND is_current='1'",s.access.current().getUserName(),cid);
        s.jdbc.update("UPDATE biz_class_group SET is_current='0',update_by=?,update_time=CURRENT_TIMESTAMP WHERE class_id=? AND is_current='1'",s.access.current().getUserName(),cid);
        Collections.shuffle(ids,new java.security.SecureRandom());
        for(int start=0;start<ids.size();start+=size) {
            long gid=s.insert(BizTable.GROUP,new LinkedHashMap<>(Map.of("class_id",cid,"group_name","第"+(start/size+1)+"组","sort",start/size,"is_current","1")));
            for(int i=start;i<Math.min(start+size,ids.size());i++)s.insert(BizTable.MEMBER,new LinkedHashMap<>(Map.of("group_id",gid,"student_id",ids.get(i),"is_current","1")));
        }
    }
    @Transactional public long addGroup(long cid,String name) {
        s.access.require("biz:pk:edit");s.classroom(cid,true);lock(cid);
        var p=new LinkedHashMap<String,Object>();p.put("group_name",name);CatalogService.str(p,"group_name",64);p.put("class_id",cid);p.put("is_current","1");return s.insert(BizTable.GROUP,p);
    }
    @Transactional public void removeGroup(long cid,long group,long target) {
        s.access.require("biz:pk:edit");s.classroom(cid,true);lock(cid);var g=s.db.one(BizTable.GROUP,group);var to=s.db.one(BizTable.GROUP,target);
        if(g==null||to==null||group==target||id(g.get("class_id"))!=cid||id(to.get("class_id"))!=cid||!"1".equals(g.get("is_current"))||!"1".equals(to.get("is_current")))throw new ServiceException("请选择本班另一个当前小组接收成员",400);
        for(long uid:s.jdbc.queryForList("SELECT student_id FROM biz_student_group WHERE group_id=? AND is_current='1' AND del_flag='0' ORDER BY student_id",Long.class,group))move(cid,uid,target);
        s.db.delete(BizTable.GROUP,group,s.access.current().getUserName());
    }
    @Transactional public void move(long cid,long uid,long group) {
        s.access.require("biz:pk:edit");s.classroom(cid,true);lock(cid);var student=s.student(uid,true);var g=s.db.one(BizTable.GROUP,group);
        if(id(student.get("class_id"))!=cid||g==null||id(g.get("class_id"))!=cid||!"1".equals(g.get("is_current")))deny();
        s.jdbc.update("UPDATE biz_student_group SET is_current='0',update_time=CURRENT_TIMESTAMP WHERE student_id=? AND is_current='1'",uid);
        s.insert(BizTable.MEMBER,new LinkedHashMap<>(Map.of("group_id",group,"student_id",uid,"is_current","1")));
    }
    public List<Map<String,Object>> rounds(long cid,long course,String mode,String stage) {
        s.access.require("biz:pk:list");s.classroom(cid,false);
        var q=new BizQuery(BizTable.ROUND).eq("class_id",cid).eq("courseware_id",course).eq("mode",mode).eq("stage",stage);q.limit=1000;
        var rounds=s.db.list(q);for(var r:rounds){var matches=new BizQuery(BizTable.MATCH).eq("round_id",r.get("round_id"));matches.limit=10000;r.put("matches",s.db.list(matches));}return rounds;
    }
    @Transactional public long match(long cid,long course,String mode,String stage) {
        s.access.require("biz:pk:edit");var c=s.course(cid,course);lock(cid);
        if(!Set.of("PERSONAL","GROUP").contains(mode)||!Set.of("BASIC","LEVEL2","LEVEL3").contains(stage))throw new ServiceException("PK模式或阶次无效",400);
        if(("PERSONAL".equals(mode)||!"MATH".equals(c.get("subject_code")))&&!"BASIC".equals(stage))throw new ServiceException("当前模式不支持阶次",400);
        if(!"BASIC".equals(stage)) {
            String prev="LEVEL3".equals(stage)?"LEVEL2":"BASIC";
            var previous=s.jdbc.queryForList("SELECT round_id FROM biz_pk_round WHERE class_id=? AND courseware_id=? AND mode='GROUP' AND stage=? AND (status='COMPLETED' OR (status='CLEARED' AND NOT EXISTS(SELECT 1 FROM biz_pk_match m WHERE m.round_id=biz_pk_round.round_id AND m.winner_id IS NULL AND m.del_flag='0'))) AND del_flag='0'",cid,course,prev);
            if(previous.isEmpty())throw new ServiceException("须先完成上一阶至少一轮完整对战",400);
        }
        var old=s.jdbc.queryForList("SELECT * FROM biz_pk_round WHERE class_id=? AND courseware_id=? AND mode=? AND stage=? AND status<>'CLEARED' AND del_flag='0' ORDER BY round_no DESC",cid,course,mode,stage);
        if(!old.isEmpty()&&"ACTIVE".equals(old.get(0).get("status")))throw new ServiceException("请先完成当前轮次",400);
        Map<Long,List<Long>> members=new LinkedHashMap<>();
        if("PERSONAL".equals(mode)) {
            for(long uid:s.jdbc.queryForList("SELECT st.user_id FROM biz_student_extend st JOIN sys_user u ON u.user_id=st.user_id WHERE st.class_id=? AND st.del_flag='0' AND u.status='0' AND u.del_flag='0' ORDER BY st.user_id",Long.class,cid))members.put(uid,List.of(uid));
        } else {
            for(var g:groups(cid)) {
                long gid=id(g.get("group_id"));var list=s.jdbc.queryForList("SELECT sg.student_id FROM biz_student_group sg JOIN biz_student_extend st ON st.user_id=sg.student_id JOIN sys_user u ON u.user_id=st.user_id WHERE sg.group_id=? AND sg.is_current='1' AND sg.del_flag='0' AND st.class_id=? AND st.del_flag='0' AND u.status='0' AND u.del_flag='0' ORDER BY sg.student_id",Long.class,gid,cid);
                if(!list.isEmpty())members.put(gid,list);
            }
        }
        var contestants=new ArrayList<>(members.keySet());int no=old.isEmpty()?1:(int)id(old.get(0).get("round_no"))+1;
        if(!old.isEmpty()) {
            var matches=s.jdbc.queryForList("SELECT * FROM biz_pk_match WHERE round_id=? AND del_flag='0'",old.get(0).get("round_id"));
            contestants=new ArrayList<>(matches.stream().map(m->id(m.get("winner_id"))).distinct().toList());
            if(!members.keySet().containsAll(contestants))throw new ServiceException("参赛成员或分组已变化，请清空赛程重新匹配",400);
            if(contestants.size()<2)throw new ServiceException("本阶赛程已结束，请清空赛程后重新开始",400);
            if(contestants.size()%2==1) {
                var losers=new ArrayList<Long>();for(var m:matches)for(String k:List.of("left_id","right_id")){long x=id(m.get(k));if(x>0&&!contestants.contains(x)&&members.containsKey(x))losers.add(x);}
                if(losers.isEmpty())throw new ServiceException("没有可补位的失败方",400);Collections.shuffle(losers);contestants.add(losers.get(0));
            }
        }
        if(contestants.size()<2)throw new ServiceException("至少需要2名学生或2个小组",400);Collections.shuffle(contestants,new java.security.SecureRandom());
        long round=s.insert(BizTable.ROUND,new LinkedHashMap<>(Map.of("class_id",cid,"courseware_id",course,"mode",mode,"stage",stage,"round_no",no,"status","ACTIVE")));
        for(int i=0;i<contestants.size();i+=2) {
            long left=contestants.get(i);Long right=i+1<contestants.size()?contestants.get(i+1):null;
            var snapshot=new LinkedHashMap<String,Object>();snapshot.put(Long.toString(left),members.get(left));if(right!=null)snapshot.put(right.toString(),members.get(right));
            var values=new LinkedHashMap<String,Object>();values.put("round_id",round);values.put("left_id",left);values.put("right_id",right);values.put("winner_id",right==null?left:null);values.put("members_json",s.encode(snapshot));s.insert(BizTable.MATCH,values);
        }
        return round;
    }
    @Transactional public void result(long match,Long winner) {
        s.access.require("biz:pk:edit");var m=s.db.one(BizTable.MATCH,match);if(m==null)throw new ServiceException("对阵不存在",404);
        var r=s.db.one(BizTable.ROUND,id(m.get("round_id")));long cid=id(r.get("class_id"));var c=s.course(cid,id(r.get("courseware_id")));lock(cid);
        m=s.jdbc.queryForMap("SELECT * FROM biz_pk_match WHERE match_id=? FOR UPDATE",match);
        r=s.db.one(BizTable.ROUND,id(m.get("round_id")));
        if("CLEARED".equals(r.get("status")))throw new ServiceException("赛程已清空",400);
        if(m.get("right_id")==null)throw new ServiceException("轮空自动晋级，无对战积分",400);
        if(winner!=null&&winner!=id(m.get("left_id"))&&winner!=id(m.get("right_id")))throw new ServiceException("获胜者不在该对阵中",400);
        if(Objects.equals(winner,m.get("winner_id"))||(winner!=null&&m.get("winner_id")!=null&&winner==id(m.get("winner_id"))))return;
        if(!s.jdbc.queryForList("SELECT round_id FROM biz_pk_round WHERE class_id=? AND courseware_id=? AND mode=? AND stage=? AND round_no>? AND status<>'CLEARED'",cid,r.get("courseware_id"),r.get("mode"),r.get("stage"),r.get("round_no")).isEmpty())throw new ServiceException("后续轮次已生成，须先清空赛程再调整积分",400);
        int version=(int)id(m.get("result_version"))+1;String stage=text(r,"stage");int delta="GROUP".equals(r.get("mode"))?s.number("biz.pk.score."+stage,"LEVEL3".equals(stage)?3:"LEVEL2".equals(stage)?2:1,1,100):1;
        if(m.get("winner_id")!=null) {
            var logs=s.jdbc.queryForList("SELECT * FROM biz_score_log WHERE event_key LIKE ? ORDER BY student_id", "PK:"+match+":"+id(m.get("result_version"))+":%");
            for(var log:logs)scores.post(id(log.get("student_id")),cid,c,stage,-(int)id(log.get("score")),"撤销对战","PKREVERSE:"+log.get("log_id"),id(log.get("log_id")));
        }
        if(winner!=null) {
            var snapshot=s.object(m.get("members_json"));var ids=(List<?>)snapshot.get(winner.toString());
            for(long uid:ids.stream().map(AccessService::id).sorted().toList())scores.post(uid,cid,c,stage,delta,"PK对战","PK:"+match+":"+version+":"+uid,null);
        }
        s.jdbc.update("UPDATE biz_pk_match SET winner_id=?,result_version=?,update_by=?,update_time=CURRENT_TIMESTAMP WHERE match_id=?",winner,version,s.access.current().getUserName(),match);
        int pending=s.jdbc.queryForObject("SELECT COUNT(*) FROM biz_pk_match WHERE round_id=? AND winner_id IS NULL",Integer.class,r.get("round_id"));
        s.jdbc.update("UPDATE biz_pk_round SET status=?,update_time=CURRENT_TIMESTAMP WHERE round_id=?",pending==0?"COMPLETED":"ACTIVE",r.get("round_id"));
    }
    @Transactional public void clear(long cid,long course,String mode,String stage) {
        s.access.require("biz:pk:edit");s.course(cid,course);lock(cid);
        s.jdbc.update("UPDATE biz_pk_round SET status='CLEARED',update_by=?,update_time=CURRENT_TIMESTAMP WHERE class_id=? AND courseware_id=? AND mode=? AND stage=?",s.access.current().getUserName(),cid,course,mode,stage);
    }
    @Transactional public void archive(List<Long> cids) {
        s.access.require("biz:class:edit");if(s.access.teacher(s.access.current())||s.access.student(s.access.current()))deny();
        if(cids.isEmpty()||cids.size()>100)throw new ServiceException("每次归档1至100个班级",400);
        for(long cid:new TreeSet<>(cids)) {
            s.classroom(cid,true);lock(cid);
            s.jdbc.update("UPDATE sys_user SET status='1',update_by=?,update_time=CURRENT_TIMESTAMP WHERE user_id IN (SELECT user_id FROM biz_student_extend WHERE class_id=? AND del_flag='0')",s.access.current().getUserName(),cid);
            s.db.update(BizTable.CLASS,cid,new LinkedHashMap<>(Map.of("status","ARCHIVED")),s.access.current().getUserName());
        }
    }
}
