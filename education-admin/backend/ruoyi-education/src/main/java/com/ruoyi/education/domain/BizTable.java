package com.ruoyi.education.domain;

import java.util.*;

/** SQL identifiers come exclusively from this registry, never from request input. */
public enum BizTable {
    SCHOOL("biz_school", "school_id", "school_name", "school_id,school_code,school_name,expire_time,school_status"),
    CLASS("biz_class", "class_id", "class_name", "school_id,academic_year,grade,class_name,status"),
    GAME("biz_game", "game_id", "game_name", "game_code,game_name,category_code,source_type,entry_path,access_policy,status,version_no,description,sort_no"),
    COURSEWARE("biz_courseware_base", "courseware_id", "courseware_name", "school_id,subject_code,volume,stage_code,courseware_name,description,status,sort_no,version_no,cover_url"),
    POOL("biz_school_resource", "resource_id", "subject_code", "school_id,subject_code,volume,courseware_id,start_time,end_time"),
    GRANT("biz_student_entitlement", "entitlement_id", "remark", "student_user_id,start_time,end_time,status,source_type,source_id"),
    STUDENT("biz_student_extend", "user_id", "student_no", "user_id,school_id,class_id,student_no,is_trial,current_volume"),
    TEACHER("biz_teacher_class", "id", "subject_code", "teacher_user_id,school_id,class_id,subject_code,start_time,end_time"),
    SECURITY("biz_account_security", "user_id", "user_id", "user_id,password_version,must_change_pwd,phone_verified"),
    MATERIAL("biz_dubbing_material", "material_id", "title", "volume,title,video_url,material_text,material_tag,status"),
    EXAM("biz_dubbing_exam_config", "config_id", "volume", "volume,extract_scope,extract_num,extract_mode,score_rule,status"),
    MATH("biz_courseware_math", "courseware_id", "ability", "courseware_id,stage,ability,discuss_img,discuss_content,intro_video,game_url,teacher_book"),
    READ("biz_courseware_read", "courseware_id", "config_json", "courseware_id,dubbing_material_id,config_json"),
    BATCH("biz_activate_code_batch", "batch_id", "batch_name", "batch_name,permission_type,permission_range,valid_days,total_num,used_num,expire_time,school_id"),
    CODE("biz_activate_code", "code_id", "code", "batch_id,code,status,student_id,use_time"),
    GROUP("biz_class_group", "group_id", "group_name", "class_id,group_name,sort,is_current"),
    MEMBER("biz_student_group", "id", "student_id", "group_id,student_id,is_current"),
    ROUND("biz_pk_round", "round_id", "mode", "class_id,courseware_id,mode,stage,round_no,lesson_id,status"),
    MATCH("biz_pk_match", "match_id", "left_id", "round_id,left_id,right_id,winner_id,members_json,result_version"),
    RECORD("biz_student_dubbing_record", "record_id", "student_id", "school_id,grade,class_id,student_id,volume,material_id,courseware_id,dubbing_type,answer_time,accuracy_score,fluency_score,completeness_score,phonology_score,intonation_score,overall_score,total_score,star_num,audio_url"),
    HISTORY("biz_student_class_history", "id", "change_reason", "student_user_id,school_id,class_id,start_time,end_time,change_reason");

    public final String table, id, name;
    public final Set<String> writable;
    BizTable(String table, String id, String name, String columns) {
        this.table = table; this.id = id; this.name = name;
        this.writable = new LinkedHashSet<>(Arrays.asList(columns.split(",")));
        this.writable.add("remark");
    }
}
