package com.ruoyi.education.service;

import com.ruoyi.education.domain.*;
import com.ruoyi.education.mapper.BizMapper;
import com.ruoyi.system.service.ISysConfigService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static com.ruoyi.education.service.AccessService.*;

/** Joins and transfers preserve old membership rows and fill current groups in class order. */
@Service
public class GroupingService {
    final JdbcTemplate jdbc;final BizMapper db;final ISysConfigService config;
    public GroupingService(JdbcTemplate jdbc,BizMapper db,ISysConfigService config){this.jdbc=jdbc;this.db=db;this.config=config;}
    @Transactional public void join(long uid,long cid,String actor) {
        jdbc.queryForList("SELECT class_id FROM biz_class WHERE class_id=? FOR UPDATE",cid);
        var existing=jdbc.queryForList("SELECT sg.id FROM biz_student_group sg JOIN biz_class_group g ON g.group_id=sg.group_id WHERE sg.student_id=? AND sg.is_current='1' AND sg.del_flag='0' AND g.class_id=? AND g.is_current='1' AND g.del_flag='0'",uid,cid);
        if(!existing.isEmpty())return;
        jdbc.update("UPDATE biz_student_group SET is_current='0',update_by=?,update_time=CURRENT_TIMESTAMP WHERE student_id=? AND is_current='1'",actor,uid);
        String setting=config.selectConfigByKey("biz.group.size");int size=setting==null||setting.isBlank()?4:Integer.parseInt(setting);
        if(size<2||size>6)throw new com.ruoyi.common.exception.ServiceException("默认小组人数须为2至6",400);
        var groups=jdbc.queryForList("SELECT g.group_id FROM biz_class_group g WHERE g.class_id=? AND g.is_current='1' AND g.del_flag='0' AND (SELECT COUNT(*) FROM biz_student_group sg JOIN biz_student_extend st ON st.user_id=sg.student_id WHERE sg.group_id=g.group_id AND sg.is_current='1' AND sg.del_flag='0' AND st.class_id=? AND st.del_flag='0')<? ORDER BY g.sort,g.group_id",cid,cid,size);
        long group;
        if(groups.isEmpty()) {
            int n=jdbc.queryForObject("SELECT COUNT(*) FROM biz_class_group WHERE class_id=? AND is_current='1' AND del_flag='0'",Integer.class,cid)+1;
            var v=new LinkedHashMap<String,Object>(Map.of("class_id",cid,"group_name","第"+n+"组","sort",n,"is_current","1"));db.insert(BizTable.GROUP,v,actor);group=id(v.get("generatedId"));
        } else group=id(groups.get(0).get("group_id"));
        db.insert(BizTable.MEMBER,new LinkedHashMap<>(Map.of("group_id",group,"student_id",uid,"is_current","1")),actor);
    }
}
