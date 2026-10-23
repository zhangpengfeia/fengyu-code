-- Education phase 1. UTC datetime. Business identity keys remain reserved after logical deletion.
CREATE TABLE biz_schema_version (
version_id bigint PRIMARY KEY, version_name varchar(128) NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_org_extend (
dept_id bigint PRIMARY KEY, org_type varchar(16) NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(dept_id) REFERENCES sys_dept(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_school (
school_id bigint PRIMARY KEY, school_code varchar(32) NOT NULL UNIQUE, school_name varchar(100) NOT NULL, expire_time datetime NOT NULL, school_status varchar(16) NOT NULL DEFAULT 'ACTIVE',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES sys_dept(dept_id), INDEX ix_school_expiry(expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_class (
class_id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NOT NULL, academic_year varchar(9) NOT NULL, grade tinyint NOT NULL, class_name varchar(64) NOT NULL, status varchar(16) NOT NULL DEFAULT 'ACTIVE',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES biz_school(school_id), UNIQUE(school_id,academic_year,grade,class_name), CHECK(grade BETWEEN 1 AND 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_account_security (
user_id bigint PRIMARY KEY, password_version int NOT NULL DEFAULT 1, must_change_pwd tinyint NOT NULL DEFAULT 1, phone_verified tinyint NOT NULL DEFAULT 0, verified_phone varchar(11) NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(user_id) REFERENCES sys_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_student_extend (
user_id bigint PRIMARY KEY, school_id bigint NOT NULL, class_id bigint NOT NULL, student_no varchar(32) NOT NULL, is_trial tinyint NOT NULL DEFAULT 0,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(user_id) REFERENCES sys_user(user_id), FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(class_id) REFERENCES biz_class(class_id), UNIQUE(school_id,student_no), INDEX ix_student_class(school_id,class_id,del_flag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_teacher_class (
id bigint PRIMARY KEY AUTO_INCREMENT, teacher_user_id bigint NOT NULL, school_id bigint NOT NULL, class_id bigint NOT NULL, subject_code varchar(16) NOT NULL DEFAULT 'ANY', start_time datetime NOT NULL, end_time datetime NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(teacher_user_id) REFERENCES sys_user(user_id), FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(class_id) REFERENCES biz_class(class_id), INDEX ix_teaching(teacher_user_id,class_id,end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_student_class_history (
id bigint PRIMARY KEY AUTO_INCREMENT, student_user_id bigint NOT NULL, school_id bigint NOT NULL, class_id bigint NOT NULL, start_time datetime NOT NULL, end_time datetime NULL, change_reason varchar(500) NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(student_user_id) REFERENCES sys_user(user_id), FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(class_id) REFERENCES biz_class(class_id), INDEX ix_history(student_user_id,start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_game (
game_id bigint PRIMARY KEY AUTO_INCREMENT, game_code varchar(64) NOT NULL UNIQUE, game_name varchar(128) NOT NULL,
category_code varchar(32) NOT NULL DEFAULT 'MATH', source_type varchar(16) NOT NULL, entry_path varchar(1024) NOT NULL,
access_policy varchar(32) NOT NULL DEFAULT 'TEACHER_ONLY', status varchar(16) NOT NULL DEFAULT 'DRAFT',
version_no varchar(32) NOT NULL DEFAULT 'v1', description varchar(4000) NOT NULL DEFAULT '', sort_no int NOT NULL DEFAULT 0,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
INDEX ix_game_catalog(status,category_code,sort_no), CHECK(source_type IN ('INTERNAL','EXTERNAL')), CHECK(access_policy IN ('TEACHER_ONLY','STUDENT','COURSEWARE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_courseware_base (
courseware_id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NULL, subject_code varchar(16) NOT NULL,
volume tinyint NOT NULL, stage_code varchar(16) NOT NULL DEFAULT 'BASIC', courseware_name varchar(128) NOT NULL,
description varchar(4000) NOT NULL DEFAULT '', status varchar(16) NOT NULL DEFAULT 'DRAFT', sort_no int NOT NULL DEFAULT 0,
version_no int NOT NULL DEFAULT 0, source_courseware_id bigint NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(source_courseware_id) REFERENCES biz_courseware_base(courseware_id), INDEX ix_courseware(school_id,subject_code,volume,status), CHECK(volume BETWEEN 1 AND 12)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_courseware_version (
id bigint PRIMARY KEY AUTO_INCREMENT, courseware_id bigint NOT NULL, version_no int NOT NULL, content_snapshot_json json NOT NULL, publisher varchar(64) NOT NULL, publish_time datetime NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), UNIQUE(courseware_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_courseware_game (
id bigint PRIMARY KEY AUTO_INCREMENT, courseware_id bigint NOT NULL, game_id bigint NOT NULL, sort_no int NOT NULL DEFAULT 0, game_mode varchar(16) NOT NULL DEFAULT 'TRAINING',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), FOREIGN KEY(game_id) REFERENCES biz_game(game_id), UNIQUE(courseware_id,game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_school_resource (
resource_id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NOT NULL, subject_code varchar(16) NULL, volume tinyint NULL, courseware_id bigint NULL, start_time datetime NOT NULL, end_time datetime NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), INDEX ix_pool(school_id,start_time,end_time), CHECK(end_time>start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_student_entitlement (
entitlement_id bigint PRIMARY KEY AUTO_INCREMENT, student_user_id bigint NOT NULL, source_type varchar(16) NOT NULL, source_id bigint NULL, start_time datetime NOT NULL, end_time datetime NOT NULL, status varchar(16) NOT NULL DEFAULT 'ACTIVE',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(student_user_id) REFERENCES sys_user(user_id), INDEX ix_entitlement(student_user_id,status,end_time), CHECK(end_time>start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE biz_entitlement_scope (
scope_id bigint PRIMARY KEY AUTO_INCREMENT, entitlement_id bigint NOT NULL, subject_code varchar(16) NULL, volume tinyint NULL, stage_code varchar(16) NULL, courseware_id bigint NULL, game_id bigint NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(entitlement_id) REFERENCES biz_student_entitlement(entitlement_id), FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), FOREIGN KEY(game_id) REFERENCES biz_game(game_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO biz_schema_version(version_id,version_name) VALUES(2,'education-phase1');
