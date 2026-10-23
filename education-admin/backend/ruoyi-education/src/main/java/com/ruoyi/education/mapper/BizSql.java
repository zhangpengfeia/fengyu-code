package com.ruoyi.education.mapper;
import com.ruoyi.education.domain.*;
import java.util.*;

public class BizSql {
    private static BizQuery q(Map<String,Object> p) { return (BizQuery)p.get("q"); }
    private static String in(String key, int count) {
        List<String> v = new ArrayList<>();
        for(int i=0;i<count;i++) v.add("#{q."+key+"["+i+"]}");
        return String.join(",",v);
    }
    private static String where(BizQuery q) {
        StringBuilder s = new StringBuilder(" WHERE del_flag='0'");
        q.filters.forEach((k,v)-> {
            if (!q.table.writable.contains(k) && !q.table.id.equals(k)) throw new IllegalArgumentException("Invalid filter");
            s.append(" AND ").append(k).append("=#{q.filters.").append(k).append("}");
        });
        if(q.keyword != null && !q.keyword.isBlank()) s.append(" AND ").append(q.table.name).append(" LIKE CONCAT('%',#{q.keyword},'%')");
        if(q.ids != null) s.append(q.ids.isEmpty()?" AND 1=0":" AND "+q.table.id+" IN ("+in("ids",q.ids.size())+")");
        if(q.schoolIds != null) {
            String scope = q.schoolIds.isEmpty()?"1=0":"school_id IN ("+in("schoolIds",q.schoolIds.size())+")";
            if(q.table == BizTable.COURSEWARE) scope="school_id IS NULL OR "+scope;
            s.append(" AND (").append(scope).append(")");
        }
        return s.toString();
    }
    public String list(Map<String,Object> p) {
        BizQuery q=q(p);
        String sort=(q.table==BizTable.GAME||q.table==BizTable.COURSEWARE)?"sort_no ASC,":"";
        return "SELECT * FROM "+q.table.table+where(q)+" ORDER BY "+sort+q.table.id+" DESC LIMIT #{q.limit} OFFSET #{q.offset}";
    }
    public String count(Map<String,Object> p) { BizQuery q=q(p); return "SELECT COUNT(*) FROM "+q.table.table+where(q); }
    public String one(Map<String,Object> p) { BizTable t=(BizTable)p.get("table"); return "SELECT * FROM "+t.table+" WHERE "+t.id+"=#{id} AND del_flag='0'"; }
    @SuppressWarnings("unchecked")
    private static List<String> keys(Map<String,Object> p) {
        BizTable t=(BizTable)p.get("table"); Map<String,Object> v=(Map<String,Object>)p.get("v");
        List<String> keys = new ArrayList<>(v.keySet());
        keys.remove("expected_row_version");
        if(keys.isEmpty() || !t.writable.containsAll(keys)) throw new IllegalArgumentException("Invalid columns");
        return keys;
    }
    public String insert(Map<String,Object> p) {
        BizTable t=(BizTable)p.get("table"); List<String> k=keys(p);
        return "INSERT INTO "+t.table+" ("+String.join(",",k)+",create_by,create_time) VALUES ("+
          String.join(",",k.stream().map(x->"#{v."+x+"}").toList())+",#{actor},CURRENT_TIMESTAMP)";
    }
    public String update(Map<String,Object> p) {
        BizTable t=(BizTable)p.get("table"); List<String> k=keys(p);
        return "UPDATE "+t.table+" SET "+String.join(",",k.stream().map(x->x+"=#{v."+x+"}").toList())+
          ",row_version=row_version+1,update_by=#{actor},update_time=CURRENT_TIMESTAMP WHERE "+t.id+"=#{id} AND del_flag='0'"+
          (((Map<?,?>)p.get("v")).containsKey("expected_row_version")?" AND row_version=#{v.expected_row_version}":"");
    }
    public String delete(Map<String,Object> p) {
        BizTable t=(BizTable)p.get("table");
        return "UPDATE "+t.table+" SET del_flag='2',update_by=#{actor},update_time=CURRENT_TIMESTAMP WHERE "+t.id+"=#{id} AND del_flag='0'";
    }
    @SuppressWarnings("unchecked")
    private String userWhere(Map<String,Object> p) {
        Map<String,Object> q=(Map<String,Object>)p.get("q");
        String s=" FROM sys_user u LEFT JOIN biz_student_extend st ON st.user_id=u.user_id AND st.del_flag='0' LEFT JOIN biz_account_security a ON a.user_id=u.user_id WHERE u.del_flag='0'";
        if(q.get("schools") instanceof List<?> schools) {
            if(schools.isEmpty())s+=" AND 1=0";
            else {List<String> placeholders=new ArrayList<>();for(int i=0;i<schools.size();i++)placeholders.add("#{q.schools["+i+"]}");s+=" AND u.dept_id IN ("+String.join(",",placeholders)+")";}
        }
        if(q.get("teacher")!=null)s+=" AND EXISTS(SELECT 1 FROM biz_teacher_class tc WHERE tc.class_id=st.class_id AND tc.teacher_user_id=#{q.teacher} AND tc.del_flag='0' AND tc.start_time<=CURRENT_TIMESTAMP AND (tc.end_time IS NULL OR tc.end_time>CURRENT_TIMESTAMP))";
        for(String key:List.of("school","class"))if(q.get(key)!=null)s+=" AND "+(key.equals("school")?"u.dept_id":"st.class_id")+"=#{q."+key+"}";
        if(q.get("role")!=null)s+=" AND EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id=ur.role_id WHERE ur.user_id=u.user_id AND r.role_key=#{q.role} AND r.status='0' AND r.del_flag='0')";
        if(q.get("keyword")!=null)s+=" AND (u.user_name LIKE CONCAT('%',#{q.keyword},'%') OR u.nick_name LIKE CONCAT('%',#{q.keyword},'%') OR u.phonenumber LIKE CONCAT('%',#{q.keyword},'%'))";
        return s;
    }
    public String userList(Map<String,Object> p) {return "SELECT u.user_id,u.dept_id,u.user_name,u.nick_name,u.phonenumber,u.status,u.create_time,st.class_id,st.student_no,a.phone_verified,a.must_change_pwd"+userWhere(p)+" ORDER BY u.user_id DESC LIMIT #{q.limit} OFFSET #{q.offset}";}
    public String userCount(Map<String,Object> p) {return "SELECT COUNT(*)"+userWhere(p);}
}
