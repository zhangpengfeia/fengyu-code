package com.ruoyi.education.mapper;

import com.ruoyi.education.domain.*;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface BizMapper {
    @Select("SELECT dict_value FROM sys_dict_data WHERE dict_type=#{type} AND status='0' ORDER BY dict_sort,dict_code") List<String> dictValues(String type);
    @Update("UPDATE sys_dept SET dept_type='1',expire_time=#{expiry},status=#{status},update_by=#{actor},update_time=CURRENT_TIMESTAMP WHERE dept_id=#{id}")
    void schoolState(@Param("id") long id,@Param("expiry") Object expiry,@Param("status") String status,@Param("actor") String actor);
    @SelectProvider(type=BizSql.class,method="userList") List<Map<String,Object>> scopedUsers(@Param("q") Map<String,Object> query);
    @SelectProvider(type=BizSql.class,method="userCount") long countUsers(@Param("q") Map<String,Object> query);
    @Select("SELECT COUNT(*) FROM sys_dept WHERE dept_id=#{id} AND del_flag='0'") int schoolDepartment(long id);
    @Select("SELECT COUNT(*) FROM biz_student_extend WHERE class_id=#{cid} AND del_flag='0'") int currentClassStudents(long cid);
    @Select("SELECT COUNT(*) FROM biz_teacher_class WHERE teacher_user_id=#{uid} AND class_id=#{cid} AND del_flag='0' AND start_time<=CURRENT_TIMESTAMP AND (end_time IS NULL OR end_time>CURRENT_TIMESTAMP)") int teaches(@Param("uid") long uid,@Param("cid") long cid);
    @Select("SELECT COUNT(*) FROM sys_dept d JOIN sys_dept s ON s.dept_id=#{school} WHERE (d.dept_id=s.dept_id OR FIND_IN_SET(d.dept_id,s.ancestors)>0) AND (d.status<>'0' OR d.del_flag<>'0')") int disabledSchoolPath(long school);
    @SelectProvider(type=BizSql.class,method="list") List<Map<String,Object>> list(@Param("q") BizQuery q);
    @SelectProvider(type=BizSql.class,method="count") long count(@Param("q") BizQuery q);
    @SelectProvider(type=BizSql.class,method="one") Map<String,Object> one(@Param("table") BizTable t,@Param("id") long id);
    @InsertProvider(type=BizSql.class,method="insert") @Options(useGeneratedKeys=true,keyProperty="v.generatedId")
    int insert(@Param("table") BizTable t,@Param("v") Map<String,Object> v,@Param("actor") String actor);
    @UpdateProvider(type=BizSql.class,method="update") int update(@Param("table") BizTable t,@Param("id") long id,@Param("v") Map<String,Object> v,@Param("actor") String actor);
    @UpdateProvider(type=BizSql.class,method="delete") int delete(@Param("table") BizTable t,@Param("id") long id,@Param("actor") String actor);

    @Select("SELECT s.* FROM biz_entitlement_scope s JOIN biz_student_entitlement e ON s.entitlement_id=e.entitlement_id WHERE e.student_user_id=#{uid} AND e.del_flag='0' AND e.status='ACTIVE' AND e.start_time<=CURRENT_TIMESTAMP AND e.end_time>CURRENT_TIMESTAMP")
    List<Map<String,Object>> scopes(long uid);
    @Select("SELECT * FROM biz_entitlement_scope WHERE entitlement_id=#{eid}") List<Map<String,Object>> grantScopes(long eid);
    @Insert("INSERT INTO biz_entitlement_scope(entitlement_id,subject_code,volume,stage_code,courseware_id,game_id) VALUES(#{eid},#{v.subject_code},#{v.volume},#{v.stage_code},#{v.courseware_id},#{v.game_id})")
    int scope(@Param("eid") long eid,@Param("v") Map<String,Object> values);
    @Select("SELECT * FROM biz_school_resource WHERE school_id=#{school} AND del_flag='0' AND start_time<=CURRENT_TIMESTAMP AND end_time>CURRENT_TIMESTAMP")
    List<Map<String,Object>> pool(long school);
    @Select("SELECT c.* FROM biz_courseware_base c JOIN biz_courseware_game g ON c.courseware_id=g.courseware_id WHERE g.game_id=#{gameId} AND c.del_flag='0' AND c.status='PUBLISHED'")
    List<Map<String,Object>> linkedCourseware(long gameId);
    @Select("SELECT game_id FROM biz_courseware_game WHERE courseware_id=#{cid} ORDER BY sort_no,id") List<Long> gameIds(long cid);
    @Delete("DELETE FROM biz_courseware_game WHERE courseware_id=#{cid}") void unlinkGames(long cid);
    @Insert("INSERT INTO biz_courseware_game(courseware_id,game_id,sort_no) VALUES(#{cid},#{gid},#{sort})") void linkGame(@Param("cid") long cid,@Param("gid") long gid,@Param("sort") int sort);
    @Insert("INSERT INTO biz_courseware_version(courseware_id,version_no,content_snapshot_json,publisher,publish_time) VALUES(#{cid},#{version},#{json},#{actor},CURRENT_TIMESTAMP)")
    void version(@Param("cid") long cid,@Param("version") int version,@Param("json") String json,@Param("actor") String actor);
    @Select("SELECT * FROM biz_courseware_version WHERE courseware_id=#{cid} ORDER BY version_no DESC") List<Map<String,Object>> versions(long cid);
    @Select("SELECT dept_id FROM sys_role_dept WHERE role_id=#{rid}") List<Long> roleDepts(long rid);
    @Select("SELECT d.dept_id FROM sys_dept d WHERE d.del_flag='0' AND (d.dept_id=#{dept} OR FIND_IN_SET(#{dept},d.ancestors)>0)") List<Long> descendants(long dept);
    @Select("SELECT s.school_id FROM biz_school s WHERE s.del_flag='0'") List<Long> schools();
    @Select("SELECT u.user_id FROM sys_user u JOIN biz_account_security a ON a.user_id=u.user_id WHERE u.phonenumber=#{phone} AND a.phone_verified=1 AND a.verified_phone=u.phonenumber AND u.del_flag='0'") Long phoneUser(String phone);
    @Update("UPDATE sys_user SET phonenumber=#{phone} WHERE user_id=#{uid}") void phone(@Param("uid") long uid,@Param("phone") String phone);
    @Update("UPDATE biz_account_security SET phone_verified=1,verified_phone=#{phone} WHERE user_id=#{uid}") void verifyPhone(@Param("uid") long uid,@Param("phone") String phone);
    @Select("SELECT user_id FROM biz_student_extend WHERE school_id=#{school} AND student_no=#{no} AND del_flag='0'") Long studentUser(@Param("school") long school,@Param("no") String no);
    @Update("UPDATE biz_account_security SET password_version=password_version+1,must_change_pwd=#{must},update_time=CURRENT_TIMESTAMP WHERE user_id=#{uid}") void bumpPassword(@Param("uid") long uid,@Param("must") boolean must);
    @Update("UPDATE biz_student_class_history SET end_time=CURRENT_TIMESTAMP WHERE student_user_id=#{uid} AND end_time IS NULL") void closeHistory(long uid);
    @Update("UPDATE biz_teacher_class SET end_time=CURRENT_TIMESTAMP WHERE teacher_user_id=#{uid} AND end_time IS NULL") void closeTeaching(long uid);
    @Select("SELECT u.user_id,u.dept_id,u.user_name,u.nick_name,u.phonenumber,u.status,u.create_time,s.class_id,s.student_no,a.phone_verified,a.must_change_pwd FROM sys_user u LEFT JOIN biz_student_extend s ON s.user_id=u.user_id AND s.del_flag='0' LEFT JOIN biz_account_security a ON a.user_id=u.user_id WHERE u.del_flag='0' ORDER BY u.user_id DESC")
    List<Map<String,Object>> userCandidates();
}
