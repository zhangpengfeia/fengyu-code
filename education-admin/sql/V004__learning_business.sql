-- V1.0 business extensions; apply once after V003. Existing data is preserved.
ALTER TABLE sys_dept ADD COLUMN dept_type char(1) NOT NULL DEFAULT '0';
ALTER TABLE sys_dept ADD COLUMN expire_time datetime NULL;
CREATE INDEX ix_dept_expiry ON sys_dept(dept_type,expire_time);
UPDATE sys_dept SET dept_type='1',expire_time=(SELECT s.expire_time FROM biz_school s WHERE s.school_id=sys_dept.dept_id) WHERE dept_id IN (SELECT school_id FROM biz_school);
ALTER TABLE biz_student_extend ADD COLUMN total_score int NOT NULL DEFAULT 0;
ALTER TABLE biz_student_extend ADD COLUMN volume_score text;
ALTER TABLE biz_student_extend ADD COLUMN grade_score text;
ALTER TABLE biz_student_extend ADD COLUMN current_volume int NOT NULL DEFAULT 1;
ALTER TABLE biz_courseware_base ADD COLUMN cover_url varchar(1024) NOT NULL DEFAULT '';
CREATE TABLE biz_dubbing_material (
material_id bigint PRIMARY KEY AUTO_INCREMENT, volume int NOT NULL, title varchar(128) NOT NULL, video_url varchar(1024) NOT NULL, material_text text NOT NULL, material_tag varchar(32) NOT NULL, status char(1) NOT NULL DEFAULT '0', UNIQUE(volume,title), CHECK(volume BETWEEN 1 AND 12), INDEX ix_material(volume,status),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_courseware_math (
courseware_id bigint PRIMARY KEY, stage varchar(32) NOT NULL, ability varchar(64) NOT NULL, discuss_img varchar(1024), discuss_content text, intro_video varchar(1024), game_url varchar(1024), teacher_book varchar(1024), FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_courseware_read (
courseware_id bigint PRIMARY KEY, dubbing_material_id bigint, config_json text NOT NULL, FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), FOREIGN KEY(dubbing_material_id) REFERENCES biz_dubbing_material(material_id),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_dubbing_exam_config (
config_id bigint PRIMARY KEY AUTO_INCREMENT, volume int NOT NULL UNIQUE, extract_scope char(1) NOT NULL DEFAULT '3', extract_num int NOT NULL DEFAULT 1, extract_mode char(1) NOT NULL DEFAULT '1', score_rule char(1) NOT NULL DEFAULT '1', status char(1) NOT NULL DEFAULT '0', CHECK(volume BETWEEN 1 AND 12), CHECK(extract_num>0),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_student_dubbing_record (
record_id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NOT NULL, grade int NOT NULL, class_id bigint NOT NULL, student_id bigint NOT NULL, volume int NOT NULL, material_id bigint NOT NULL, courseware_id bigint, dubbing_type char(1) NOT NULL, answer_time datetime NOT NULL, accuracy_score int NOT NULL, fluency_score int NOT NULL, completeness_score int NOT NULL, phonology_score int NOT NULL, intonation_score int NOT NULL, overall_score int NOT NULL, total_score int NOT NULL, star_num int, audio_url varchar(1024) NOT NULL, FOREIGN KEY(material_id) REFERENCES biz_dubbing_material(material_id), FOREIGN KEY(student_id) REFERENCES biz_student_extend(user_id), INDEX ix_dubbing(student_id,volume,dubbing_type,answer_time), INDEX ix_dubbing_class(school_id,class_id),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_activate_code_batch (
batch_id bigint PRIMARY KEY AUTO_INCREMENT, batch_name varchar(128) NOT NULL, permission_type char(1) NOT NULL, permission_range text NOT NULL, valid_days int NOT NULL DEFAULT 180, total_num int NOT NULL, used_num int NOT NULL DEFAULT 0, expire_time datetime NOT NULL, school_id bigint, INDEX ix_batch_expiry(expire_time),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_activate_code (
code_id bigint PRIMARY KEY AUTO_INCREMENT, batch_id bigint NOT NULL, code varchar(64) NOT NULL UNIQUE, status char(1) NOT NULL DEFAULT '0', student_id bigint, use_time datetime, FOREIGN KEY(batch_id) REFERENCES biz_activate_code_batch(batch_id), INDEX ix_code_batch(batch_id,status),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_class_group (
group_id bigint PRIMARY KEY AUTO_INCREMENT, class_id bigint NOT NULL, group_name varchar(64) NOT NULL, sort int NOT NULL DEFAULT 0, is_current char(1) NOT NULL DEFAULT '1', FOREIGN KEY(class_id) REFERENCES biz_class(class_id), INDEX ix_group_class(class_id,is_current),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_student_group (
id bigint PRIMARY KEY AUTO_INCREMENT, group_id bigint NOT NULL, student_id bigint NOT NULL, is_current char(1) NOT NULL DEFAULT '1', FOREIGN KEY(group_id) REFERENCES biz_class_group(group_id), FOREIGN KEY(student_id) REFERENCES biz_student_extend(user_id), INDEX ix_group_student(student_id,is_current),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_score_log (
log_id bigint PRIMARY KEY AUTO_INCREMENT, student_id bigint NOT NULL, school_id bigint NOT NULL, class_id bigint NOT NULL, grade int NOT NULL, courseware_id bigint NOT NULL, subject varchar(16) NOT NULL, volume int NOT NULL, stage varchar(16) NOT NULL DEFAULT 'BASIC', lesson_id bigint, score int NOT NULL, source varchar(64) NOT NULL, operator bigint NOT NULL, event_key varchar(128) NOT NULL UNIQUE, reversal_of bigint UNIQUE, FOREIGN KEY(student_id) REFERENCES biz_student_extend(user_id), FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), INDEX ix_score_class(school_id,class_id,courseware_id), INDEX ix_score_student(student_id,volume),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_pk_round (
round_id bigint PRIMARY KEY AUTO_INCREMENT, class_id bigint NOT NULL, courseware_id bigint NOT NULL, mode varchar(16) NOT NULL, stage varchar(16) NOT NULL, round_no int NOT NULL, lesson_id bigint, status varchar(16) NOT NULL DEFAULT 'ACTIVE', INDEX ix_round(class_id,courseware_id,mode,stage,status),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_pk_match (
match_id bigint PRIMARY KEY AUTO_INCREMENT, round_id bigint NOT NULL, left_id bigint NOT NULL, right_id bigint, winner_id bigint, members_json text NOT NULL, result_version int NOT NULL DEFAULT 0, FOREIGN KEY(round_id) REFERENCES biz_pk_round(round_id), INDEX ix_match_round(round_id),
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO biz_dubbing_exam_config(volume) VALUES(1);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(2);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(3);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(4);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(5);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(6);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(7);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(8);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(9);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(10);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(11);
INSERT INTO biz_dubbing_exam_config(volume) VALUES(12);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('学生注册是否需要审核','biz.student.register.audit','false','N','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('激活码绑定学校开关','biz.activation.schoolBound','false','N','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('默认每组人数 2至6','biz.group.size','4','N','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('新手积分','biz.pk.score.BASIC','1','N','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('能手积分','biz.pk.score.LEVEL2','2','N','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('大师积分','biz.pk.score.LEVEL3','3','N','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('1至3星评分门槛','biz.dubbing.starThresholds','60,75,90','N','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_config(config_name,config_key,config_value,config_type,create_by,create_time) VALUES('百度评分服务地址 由实际开通接口协议适配','biz.dubbing.baidu.endpoint','','N','migration',CURRENT_TIMESTAMP);
INSERT INTO biz_schema_version(version_id,version_name) VALUES(4,'education-v1-business');

CREATE TABLE biz_dubbing_exam_ticket (
ticket_id varchar(64) PRIMARY KEY, student_id bigint NOT NULL, material_id bigint NOT NULL, expires_at datetime NOT NULL, used_record_id bigint,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '', del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0,
FOREIGN KEY(student_id) REFERENCES biz_student_extend(user_id), FOREIGN KEY(material_id) REFERENCES biz_dubbing_material(material_id), INDEX ix_exam_ticket(student_id,expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE biz_school_expiry_notice (
id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NOT NULL, expire_time datetime NOT NULL, days_before int NOT NULL, notice_id bigint NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '', del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0,
UNIQUE(school_id,expire_time,days_before)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE biz_notice_recipient (
id bigint PRIMARY KEY AUTO_INCREMENT, notice_id bigint NOT NULL, user_id bigint NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '', del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0,
UNIQUE(notice_id,user_id), INDEX ix_recipient(user_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE biz_courseware_base ADD COLUMN school_scope bigint GENERATED ALWAYS AS (COALESCE(school_id,0));
CREATE UNIQUE INDEX uk_course_scope_name ON biz_courseware_base(school_scope,subject_code,volume,courseware_name);
-- Existing phase-one resources receive valid extension rows without rewriting their public identity.
INSERT INTO biz_courseware_math(courseware_id,stage,ability,create_by)
SELECT courseware_id,stage_code,'NUMBER','migration' FROM biz_courseware_base WHERE subject_code='MATH';
INSERT INTO biz_courseware_read(courseware_id,config_json,create_by)
SELECT courseware_id,'{}','migration' FROM biz_courseware_base WHERE subject_code='READING';
