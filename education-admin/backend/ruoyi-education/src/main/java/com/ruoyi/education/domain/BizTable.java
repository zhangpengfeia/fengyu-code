package com.ruoyi.education.domain;

import java.util.*;

/** SQL identifiers come exclusively from this registry, never from request input. */
public enum BizTable {
    SCHOOL("biz_school", "school_id", "school_name", "school_id,school_code,school_name,expire_time,school_status"),
    CLASS("biz_class", "class_id", "class_name", "school_id,academic_year,grade,class_name,status"),
    GAME("biz_game", "game_id", "game_name", "game_code,game_name,category_code,source_type,entry_path,access_policy,status,version_no,description,sort_no"),
    COURSEWARE("biz_courseware_base", "courseware_id", "courseware_name", "school_id,subject_code,volume,stage_code,courseware_name,description,status,sort_no,version_no"),
    POOL("biz_school_resource", "resource_id", "subject_code", "school_id,subject_code,volume,courseware_id,start_time,end_time"),
    GRANT("biz_student_entitlement", "entitlement_id", "remark", "student_user_id,start_time,end_time,status,source_type,source_id"),
    STUDENT("biz_student_extend", "user_id", "student_no", "user_id,school_id,class_id,student_no,is_trial"),
    TEACHER("biz_teacher_class", "id", "subject_code", "teacher_user_id,school_id,class_id,subject_code,start_time,end_time"),
    SECURITY("biz_account_security", "user_id", "user_id", "user_id,password_version,must_change_pwd,phone_verified"),
    HISTORY("biz_student_class_history", "id", "change_reason", "student_user_id,school_id,class_id,start_time,end_time,change_reason");

    public final String table, id, name;
    public final Set<String> writable;
    BizTable(String table, String id, String name, String columns) {
        this.table = table; this.id = id; this.name = name;
        this.writable = new LinkedHashSet<>(Arrays.asList(columns.split(",")));
        this.writable.add("remark");
    }
}
