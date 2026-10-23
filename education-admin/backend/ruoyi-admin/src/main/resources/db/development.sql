CREATE ALIAS IF NOT EXISTS FIND_IN_SET FOR 'com.ruoyi.education.config.H2Functions.findInSet';
-- Official MIT RuoYi baseline. Fresh database only. DROP statements and shared-password accounts removed.
-- ----------------------------
-- 1、部门表
-- ----------------------------
create table sys_dept (
  dept_id           bigint      not null auto_increment    ,
  parent_id         bigint      default 0                  ,
  ancestors         varchar(50)     default ''                 ,
  dept_name         varchar(30)     default ''                 ,
  order_num         int          default 0                  ,
  leader            varchar(20)     default null               ,
  phone             varchar(11)     default null               ,
  email             varchar(50)     default null               ,
  status            char(1)         default '0'                ,
  del_flag          char(1)         default '0'                ,
  create_by         varchar(64)     default ''                 ,
  create_time 	    datetime                                   ,
  update_by         varchar(64)     default ''                 ,
  update_time       datetime                                   ,
  primary key (dept_id)
);

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------
insert into sys_dept values(100,  0,   '0',          '若依科技',   0, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(101,  100, '0,100',      '深圳总公司', 1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(102,  100, '0,100',      '长沙分公司', 2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(103,  101, '0,100,101',  '研发部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(104,  101, '0,100,101',  '市场部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(105,  101, '0,100,101',  '测试部门',   3, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(106,  101, '0,100,101',  '财务部门',   4, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(107,  101, '0,100,101',  '运维部门',   5, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(108,  102, '0,100,102',  '市场部门',   1, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);
insert into sys_dept values(109,  102, '0,100,102',  '财务部门',   2, '若依', '15888888888', 'ry@qq.com', '0', '0', 'admin', CURRENT_TIMESTAMP, '', null);


-- ----------------------------
-- 2、用户信息表
-- ----------------------------
create table sys_user (
  user_id           bigint      not null auto_increment    ,
  dept_id           bigint      default null               ,
  user_name         varchar(30)     not null                   ,
  nick_name         varchar(30)     not null                   ,
  user_type         varchar(2)      default '00'               ,
  email             varchar(50)     default ''                 ,
  phonenumber       varchar(11)     default null                 ,
  sex               char(1)         default '0'                ,
  avatar            varchar(100)    default ''                 ,
  password          varchar(100)    default ''                 ,
  status            char(1)         default '0'                ,
  del_flag          char(1)         default '0'                ,
  login_ip          varchar(128)    default ''                 ,
  login_date        datetime                                   ,
  pwd_update_date   datetime                                   ,
  create_by         varchar(64)     default ''                 ,
  create_time       datetime                                   ,
  update_by         varchar(64)     default ''                 ,
  update_time       datetime                                   ,
  remark            varchar(500)    default null               ,
  primary key (user_id)
);

-- ----------------------------
-- 初始化-用户信息表数据
-- ----------------------------


-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
create table sys_post
(
  post_id       bigint      not null auto_increment    ,
  post_code     varchar(64)     not null                   ,
  post_name     varchar(50)     not null                   ,
  post_sort     int          not null                   ,
  status        char(1)         not null                   ,
  create_by     varchar(64)     default ''                 ,
  create_time   datetime                                   ,
  update_by     varchar(64)     default ''			       ,
  update_time   datetime                                   ,
  remark        varchar(500)    default null               ,
  primary key (post_id)
);

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1, 'ceo',  '董事长',    1, '0', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_post values(2, 'se',   '项目经理',  2, '0', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_post values(3, 'hr',   '人力资源',  3, '0', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_post values(4, 'user', '普通员工',  4, '0', 'admin', CURRENT_TIMESTAMP, '', null, '');


-- ----------------------------
-- 4、角色信息表
-- ----------------------------
create table sys_role (
  role_id              bigint      not null auto_increment    ,
  role_name            varchar(30)     not null                   ,
  role_key             varchar(100)    not null                   ,
  role_sort            int          not null                   ,
  data_scope           char(1)         default '1'                ,
  menu_check_strictly  tinyint      default 1                  ,
  dept_check_strictly  tinyint      default 1                  ,
  status               char(1)         not null                   ,
  del_flag             char(1)         default '0'                ,
  create_by            varchar(64)     default ''                 ,
  create_time          datetime                                   ,
  update_by            varchar(64)     default ''                 ,
  update_time          datetime                                   ,
  remark               varchar(500)    default null               ,
  primary key (role_id)
);

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values('1', '超级管理员',  'admin',  1, 1, 1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP, '', null, '超级管理员');
insert into sys_role values('2', '普通角色',    'common', 2, 2, 1, 1, '0', '0', 'admin', CURRENT_TIMESTAMP, '', null, '普通角色');


-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
create table sys_menu (
  menu_id           bigint      not null auto_increment    ,
  menu_name         varchar(50)     not null                   ,
  parent_id         bigint      default 0                  ,
  order_num         int          default 0                  ,
  path              varchar(200)    default ''                 ,
  component         varchar(255)    default null               ,
  query             varchar(255)    default null               ,
  route_name        varchar(50)     default ''                 ,
  is_frame          int          default 1                  ,
  is_cache          int          default 0                  ,
  menu_type         char(1)         default ''                 ,
  visible           char(1)         default 0                  ,
  status            char(1)         default 0                  ,
  perms             varchar(100)    default null               ,
  icon              varchar(100)    default '#'                ,
  create_by         varchar(64)     default ''                 ,
  create_time       datetime                                   ,
  update_by         varchar(64)     default ''                 ,
  update_time       datetime                                   ,
  remark            varchar(500)    default ''                 ,
  primary key (menu_id)
);

-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单
insert into sys_menu values('1', '系统管理', '0', '1', 'system',           null, '', '', 1, 0, 'M', '0', '0', '', 'system',   'admin', CURRENT_TIMESTAMP, '', null, '系统管理目录');
insert into sys_menu values('2', '系统监控', '0', '2', 'monitor',          null, '', '', 1, 0, 'M', '0', '0', '', 'monitor',  'admin', CURRENT_TIMESTAMP, '', null, '系统监控目录');
insert into sys_menu values('3', '系统工具', '0', '3', 'tool',             null, '', '', 1, 0, 'M', '0', '0', '', 'tool',     'admin', CURRENT_TIMESTAMP, '', null, '系统工具目录');
insert into sys_menu values('4', '若依官网', '0', '4', 'http://ruoyi.vip', null, '', '', 0, 0, 'M', '0', '0', '', 'guide',    'admin', CURRENT_TIMESTAMP, '', null, '若依官网地址');
-- 二级菜单
insert into sys_menu values('100',  '用户管理', '1',   '1', 'user',       'system/user/index',        '', '', 1, 0, 'C', '0', '0', 'system:user:list',        'user',          'admin', CURRENT_TIMESTAMP, '', null, '用户管理菜单');
insert into sys_menu values('101',  '角色管理', '1',   '2', 'role',       'system/role/index',        '', '', 1, 0, 'C', '0', '0', 'system:role:list',        'peoples',       'admin', CURRENT_TIMESTAMP, '', null, '角色管理菜单');
insert into sys_menu values('102',  '菜单管理', '1',   '3', 'menu',       'system/menu/index',        '', '', 1, 0, 'C', '0', '0', 'system:menu:list',        'tree-table',    'admin', CURRENT_TIMESTAMP, '', null, '菜单管理菜单');
insert into sys_menu values('103',  '部门管理', '1',   '4', 'dept',       'system/dept/index',        '', '', 1, 0, 'C', '0', '0', 'system:dept:list',        'tree',          'admin', CURRENT_TIMESTAMP, '', null, '部门管理菜单');
insert into sys_menu values('104',  '岗位管理', '1',   '5', 'post',       'system/post/index',        '', '', 1, 0, 'C', '0', '0', 'system:post:list',        'post',          'admin', CURRENT_TIMESTAMP, '', null, '岗位管理菜单');
insert into sys_menu values('105',  '字典管理', '1',   '6', 'dict',       'system/dict/index',        '', '', 1, 0, 'C', '0', '0', 'system:dict:list',        'dict',          'admin', CURRENT_TIMESTAMP, '', null, '字典管理菜单');
insert into sys_menu values('106',  '参数设置', '1',   '7', 'config',     'system/config/index',      '', '', 1, 0, 'C', '0', '0', 'system:config:list',      'edit',          'admin', CURRENT_TIMESTAMP, '', null, '参数设置菜单');
insert into sys_menu values('107',  '通知公告', '1',   '8', 'notice',     'system/notice/index',      '', '', 1, 0, 'C', '0', '0', 'system:notice:list',      'message',       'admin', CURRENT_TIMESTAMP, '', null, '通知公告菜单');
insert into sys_menu values('108',  '日志管理', '1',   '9', 'log',        '',                         '', '', 1, 0, 'M', '0', '0', '',                        'log',           'admin', CURRENT_TIMESTAMP, '', null, '日志管理菜单');
insert into sys_menu values('109',  '在线用户', '2',   '1', 'online',     'monitor/online/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:online:list',     'online',        'admin', CURRENT_TIMESTAMP, '', null, '在线用户菜单');
insert into sys_menu values('110',  '定时任务', '2',   '2', 'job',        'monitor/job/index',        '', '', 1, 0, 'C', '0', '0', 'monitor:job:list',        'job',           'admin', CURRENT_TIMESTAMP, '', null, '定时任务菜单');
insert into sys_menu values('111',  '数据监控', '2',   '3', 'druid',      'monitor/druid/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:druid:list',      'druid',         'admin', CURRENT_TIMESTAMP, '', null, '数据监控菜单');
insert into sys_menu values('112',  '服务监控', '2',   '4', 'server',     'monitor/server/index',     '', '', 1, 0, 'C', '0', '0', 'monitor:server:list',     'server',        'admin', CURRENT_TIMESTAMP, '', null, '服务监控菜单');
insert into sys_menu values('113',  '缓存监控', '2',   '5', 'cache',      'monitor/cache/index',      '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis',         'admin', CURRENT_TIMESTAMP, '', null, '缓存监控菜单');
insert into sys_menu values('114',  '缓存列表', '2',   '6', 'cacheList',  'monitor/cache/list',       '', '', 1, 0, 'C', '0', '0', 'monitor:cache:list',      'redis-list',    'admin', CURRENT_TIMESTAMP, '', null, '缓存列表菜单');
insert into sys_menu values('115',  '表单构建', '3',   '1', 'build',      'tool/build/index',         '', '', 1, 0, 'C', '0', '0', 'tool:build:list',         'build',         'admin', CURRENT_TIMESTAMP, '', null, '表单构建菜单');
insert into sys_menu values('116',  '代码生成', '3',   '2', 'gen',        'tool/gen/index',           '', '', 1, 0, 'C', '0', '0', 'tool:gen:list',           'code',          'admin', CURRENT_TIMESTAMP, '', null, '代码生成菜单');
insert into sys_menu values('117',  '系统接口', '3',   '3', 'swagger',    'tool/swagger/index',       '', '', 1, 0, 'C', '0', '0', 'tool:swagger:list',       'swagger',       'admin', CURRENT_TIMESTAMP, '', null, '系统接口菜单');
-- 三级菜单
insert into sys_menu values('500',  '操作日志', '108', '1', 'operlog',    'monitor/operlog/index',    '', '', 1, 0, 'C', '0', '0', 'monitor:operlog:list',    'form',          'admin', CURRENT_TIMESTAMP, '', null, '操作日志菜单');
insert into sys_menu values('501',  '登录日志', '108', '2', 'logininfor', 'monitor/logininfor/index', '', '', 1, 0, 'C', '0', '0', 'monitor:logininfor:list', 'logininfor',    'admin', CURRENT_TIMESTAMP, '', null, '登录日志菜单');
-- 用户管理按钮
insert into sys_menu values('1000', '用户查询', '100', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:query',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1001', '用户新增', '100', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:add',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1002', '用户修改', '100', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:edit',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1003', '用户删除', '100', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:remove',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1004', '用户导出', '100', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:export',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1005', '用户导入', '100', '6',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:import',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1006', '重置密码', '100', '7',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:user:resetPwd',       '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 角色管理按钮
insert into sys_menu values('1007', '角色查询', '101', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:query',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1008', '角色新增', '101', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:add',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1009', '角色修改', '101', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:edit',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1010', '角色删除', '101', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:remove',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1011', '角色导出', '101', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:role:export',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 菜单管理按钮
insert into sys_menu values('1012', '菜单查询', '102', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:query',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1013', '菜单新增', '102', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:add',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1014', '菜单修改', '102', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:edit',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1015', '菜单删除', '102', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:menu:remove',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 部门管理按钮
insert into sys_menu values('1016', '部门查询', '103', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:query',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1017', '部门新增', '103', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:add',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1018', '部门修改', '103', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:edit',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1019', '部门删除', '103', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:dept:remove',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 岗位管理按钮
insert into sys_menu values('1020', '岗位查询', '104', '1',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:query',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1021', '岗位新增', '104', '2',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:add',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1022', '岗位修改', '104', '3',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:edit',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1023', '岗位删除', '104', '4',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:remove',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1024', '岗位导出', '104', '5',  '', '', '', '', 1, 0, 'F', '0', '0', 'system:post:export',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 字典管理按钮
insert into sys_menu values('1025', '字典查询', '105', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:query',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1026', '字典新增', '105', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:add',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1027', '字典修改', '105', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:edit',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1028', '字典删除', '105', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:remove',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1029', '字典导出', '105', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:dict:export',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 参数设置按钮
insert into sys_menu values('1030', '参数查询', '106', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:query',        '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1031', '参数新增', '106', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:add',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1032', '参数修改', '106', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:edit',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1033', '参数删除', '106', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:remove',       '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1034', '参数导出', '106', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:config:export',       '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 通知公告按钮
insert into sys_menu values('1035', '公告查询', '107', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:query',        '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1036', '公告新增', '107', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:add',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1037', '公告修改', '107', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:edit',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1038', '公告删除', '107', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'system:notice:remove',       '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 操作日志按钮
insert into sys_menu values('1039', '操作查询', '500', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:query',      '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1040', '操作删除', '500', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:remove',     '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1041', '日志导出', '500', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:operlog:export',     '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 登录日志按钮
insert into sys_menu values('1042', '登录查询', '501', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:query',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1043', '登录删除', '501', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:remove',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1044', '日志导出', '501', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:export',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1045', '账户解锁', '501', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:logininfor:unlock',  '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 在线用户按钮
insert into sys_menu values('1046', '在线查询', '109', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:query',       '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1047', '批量强退', '109', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:batchLogout', '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1048', '单条强退', '109', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:online:forceLogout', '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 定时任务按钮
insert into sys_menu values('1049', '任务查询', '110', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:query',          '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1050', '任务新增', '110', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:add',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1051', '任务修改', '110', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:edit',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1052', '任务删除', '110', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:remove',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1053', '状态修改', '110', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:changeStatus',   '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1054', '任务导出', '110', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'monitor:job:export',         '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
-- 代码生成按钮
insert into sys_menu values('1055', '生成查询', '116', '1', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:query',             '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1056', '生成修改', '116', '2', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:edit',              '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1057', '生成删除', '116', '3', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:remove',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1058', '导入代码', '116', '4', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:import',            '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1059', '预览代码', '116', '5', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:preview',           '#', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_menu values('1060', '生成代码', '116', '6', '#', '', '', '', 1, 0, 'F', '0', '0', 'tool:gen:code',              '#', 'admin', CURRENT_TIMESTAMP, '', null, '');


-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
create table sys_user_role (
  user_id   bigint not null ,
  role_id   bigint not null ,
  primary key(user_id, role_id)
);

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------


-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
create table sys_role_menu (
  role_id   bigint not null ,
  menu_id   bigint not null ,
  primary key(role_id, menu_id)
);

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------
insert into sys_role_menu values ('2', '1');
insert into sys_role_menu values ('2', '2');
insert into sys_role_menu values ('2', '3');
insert into sys_role_menu values ('2', '4');
insert into sys_role_menu values ('2', '100');
insert into sys_role_menu values ('2', '101');
insert into sys_role_menu values ('2', '102');
insert into sys_role_menu values ('2', '103');
insert into sys_role_menu values ('2', '104');
insert into sys_role_menu values ('2', '105');
insert into sys_role_menu values ('2', '106');
insert into sys_role_menu values ('2', '107');
insert into sys_role_menu values ('2', '108');
insert into sys_role_menu values ('2', '109');
insert into sys_role_menu values ('2', '110');
insert into sys_role_menu values ('2', '111');
insert into sys_role_menu values ('2', '112');
insert into sys_role_menu values ('2', '113');
insert into sys_role_menu values ('2', '114');
insert into sys_role_menu values ('2', '115');
insert into sys_role_menu values ('2', '116');
insert into sys_role_menu values ('2', '117');
insert into sys_role_menu values ('2', '500');
insert into sys_role_menu values ('2', '501');
insert into sys_role_menu values ('2', '1000');
insert into sys_role_menu values ('2', '1001');
insert into sys_role_menu values ('2', '1002');
insert into sys_role_menu values ('2', '1003');
insert into sys_role_menu values ('2', '1004');
insert into sys_role_menu values ('2', '1005');
insert into sys_role_menu values ('2', '1006');
insert into sys_role_menu values ('2', '1007');
insert into sys_role_menu values ('2', '1008');
insert into sys_role_menu values ('2', '1009');
insert into sys_role_menu values ('2', '1010');
insert into sys_role_menu values ('2', '1011');
insert into sys_role_menu values ('2', '1012');
insert into sys_role_menu values ('2', '1013');
insert into sys_role_menu values ('2', '1014');
insert into sys_role_menu values ('2', '1015');
insert into sys_role_menu values ('2', '1016');
insert into sys_role_menu values ('2', '1017');
insert into sys_role_menu values ('2', '1018');
insert into sys_role_menu values ('2', '1019');
insert into sys_role_menu values ('2', '1020');
insert into sys_role_menu values ('2', '1021');
insert into sys_role_menu values ('2', '1022');
insert into sys_role_menu values ('2', '1023');
insert into sys_role_menu values ('2', '1024');
insert into sys_role_menu values ('2', '1025');
insert into sys_role_menu values ('2', '1026');
insert into sys_role_menu values ('2', '1027');
insert into sys_role_menu values ('2', '1028');
insert into sys_role_menu values ('2', '1029');
insert into sys_role_menu values ('2', '1030');
insert into sys_role_menu values ('2', '1031');
insert into sys_role_menu values ('2', '1032');
insert into sys_role_menu values ('2', '1033');
insert into sys_role_menu values ('2', '1034');
insert into sys_role_menu values ('2', '1035');
insert into sys_role_menu values ('2', '1036');
insert into sys_role_menu values ('2', '1037');
insert into sys_role_menu values ('2', '1038');
insert into sys_role_menu values ('2', '1039');
insert into sys_role_menu values ('2', '1040');
insert into sys_role_menu values ('2', '1041');
insert into sys_role_menu values ('2', '1042');
insert into sys_role_menu values ('2', '1043');
insert into sys_role_menu values ('2', '1044');
insert into sys_role_menu values ('2', '1045');
insert into sys_role_menu values ('2', '1046');
insert into sys_role_menu values ('2', '1047');
insert into sys_role_menu values ('2', '1048');
insert into sys_role_menu values ('2', '1049');
insert into sys_role_menu values ('2', '1050');
insert into sys_role_menu values ('2', '1051');
insert into sys_role_menu values ('2', '1052');
insert into sys_role_menu values ('2', '1053');
insert into sys_role_menu values ('2', '1054');
insert into sys_role_menu values ('2', '1055');
insert into sys_role_menu values ('2', '1056');
insert into sys_role_menu values ('2', '1057');
insert into sys_role_menu values ('2', '1058');
insert into sys_role_menu values ('2', '1059');
insert into sys_role_menu values ('2', '1060');

-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
create table sys_role_dept (
  role_id   bigint not null ,
  dept_id   bigint not null ,
  primary key(role_id, dept_id)
);

-- ----------------------------
-- 初始化-角色和部门关联表数据
-- ----------------------------
insert into sys_role_dept values ('2', '100');
insert into sys_role_dept values ('2', '101');
insert into sys_role_dept values ('2', '105');


-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
create table sys_user_post
(
  user_id   bigint not null ,
  post_id   bigint not null ,
  primary key (user_id, post_id)
);

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------


-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
create table sys_oper_log (
  oper_id           bigint      not null auto_increment    ,
  title             varchar(50)     default ''                 ,
  business_type     int          default 0                  ,
  method            varchar(200)    default ''                 ,
  request_method    varchar(10)     default ''                 ,
  operator_type     int          default 0                  ,
  oper_name         varchar(50)     default ''                 ,
  dept_name         varchar(50)     default ''                 ,
  oper_url          varchar(255)    default ''                 ,
  oper_ip           varchar(128)    default ''                 ,
  oper_location     varchar(255)    default ''                 ,
  oper_param        varchar(2000)   default ''                 ,
  json_result       varchar(2000)   default ''                 ,
  status            int          default 0                  ,
  error_msg         varchar(2000)   default ''                 ,
  oper_time         datetime                                   ,
  cost_time         bigint      default 0                  ,
  primary key (oper_id),
  key idx_sys_oper_log_bt (business_type),
  key idx_sys_oper_log_s  (status),
  key idx_sys_oper_log_ot (oper_time)
);


-- ----------------------------
-- 11、字典类型表
-- ----------------------------
create table sys_dict_type
(
  dict_id          bigint      not null auto_increment    ,
  dict_name        varchar(100)    default ''                 ,
  dict_type        varchar(100)    default ''                 ,
  status           char(1)         default '0'                ,
  create_by        varchar(64)     default ''                 ,
  create_time      datetime                                   ,
  update_by        varchar(64)     default ''                 ,
  update_time      datetime                                   ,
  remark           varchar(500)    default null               ,
  primary key (dict_id),
  unique (dict_type)
);

insert into sys_dict_type values(1,  '用户性别', 'sys_user_sex',        '0', 'admin', CURRENT_TIMESTAMP, '', null, '用户性别列表');
insert into sys_dict_type values(2,  '菜单状态', 'sys_show_hide',       '0', 'admin', CURRENT_TIMESTAMP, '', null, '菜单状态列表');
insert into sys_dict_type values(3,  '系统开关', 'sys_normal_disable',  '0', 'admin', CURRENT_TIMESTAMP, '', null, '系统开关列表');
insert into sys_dict_type values(4,  '任务状态', 'sys_job_status',      '0', 'admin', CURRENT_TIMESTAMP, '', null, '任务状态列表');
insert into sys_dict_type values(5,  '任务分组', 'sys_job_group',       '0', 'admin', CURRENT_TIMESTAMP, '', null, '任务分组列表');
insert into sys_dict_type values(6,  '系统是否', 'sys_yes_no',          '0', 'admin', CURRENT_TIMESTAMP, '', null, '系统是否列表');
insert into sys_dict_type values(7,  '通知类型', 'sys_notice_type',     '0', 'admin', CURRENT_TIMESTAMP, '', null, '通知类型列表');
insert into sys_dict_type values(8,  '通知状态', 'sys_notice_status',   '0', 'admin', CURRENT_TIMESTAMP, '', null, '通知状态列表');
insert into sys_dict_type values(9,  '操作类型', 'sys_oper_type',       '0', 'admin', CURRENT_TIMESTAMP, '', null, '操作类型列表');
insert into sys_dict_type values(10, '系统状态', 'sys_common_status',   '0', 'admin', CURRENT_TIMESTAMP, '', null, '登录状态列表');


-- ----------------------------
-- 12、字典数据表
-- ----------------------------
create table sys_dict_data
(
  dict_code        bigint      not null auto_increment    ,
  dict_sort        int          default 0                  ,
  dict_label       varchar(100)    default ''                 ,
  dict_value       varchar(100)    default ''                 ,
  dict_type        varchar(100)    default ''                 ,
  css_class        varchar(100)    default null               ,
  list_class       varchar(100)    default null               ,
  is_default       char(1)         default 'N'                ,
  status           char(1)         default '0'                ,
  create_by        varchar(64)     default ''                 ,
  create_time      datetime                                   ,
  update_by        varchar(64)     default ''                 ,
  update_time      datetime                                   ,
  remark           varchar(500)    default null               ,
  primary key (dict_code)
);

insert into sys_dict_data values(1,  1,  '男',       '0',       'sys_user_sex',        '',   '',        'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '性别男');
insert into sys_dict_data values(2,  2,  '女',       '1',       'sys_user_sex',        '',   '',        'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '性别女');
insert into sys_dict_data values(3,  3,  '未知',     '2',       'sys_user_sex',        '',   '',        'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '性别未知');
insert into sys_dict_data values(4,  1,  '显示',     '0',       'sys_show_hide',       '',   'primary', 'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '显示菜单');
insert into sys_dict_data values(5,  2,  '隐藏',     '1',       'sys_show_hide',       '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '隐藏菜单');
insert into sys_dict_data values(6,  1,  '正常',     '0',       'sys_normal_disable',  '',   'primary', 'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '正常状态');
insert into sys_dict_data values(7,  2,  '停用',     '1',       'sys_normal_disable',  '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '停用状态');
insert into sys_dict_data values(8,  1,  '正常',     '0',       'sys_job_status',      '',   'primary', 'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '正常状态');
insert into sys_dict_data values(9,  2,  '暂停',     '1',       'sys_job_status',      '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '停用状态');
insert into sys_dict_data values(10, 1,  '默认',     'DEFAULT', 'sys_job_group',       '',   '',        'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '默认分组');
insert into sys_dict_data values(11, 2,  '系统',     'SYSTEM',  'sys_job_group',       '',   '',        'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '系统分组');
insert into sys_dict_data values(12, 1,  '是',       'Y',       'sys_yes_no',          '',   'primary', 'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '系统默认是');
insert into sys_dict_data values(13, 2,  '否',       'N',       'sys_yes_no',          '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '系统默认否');
insert into sys_dict_data values(14, 1,  '通知',     '1',       'sys_notice_type',     '',   'warning', 'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '通知');
insert into sys_dict_data values(15, 2,  '公告',     '2',       'sys_notice_type',     '',   'success', 'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '公告');
insert into sys_dict_data values(16, 1,  '正常',     '0',       'sys_notice_status',   '',   'primary', 'Y', '0', 'admin', CURRENT_TIMESTAMP, '', null, '正常状态');
insert into sys_dict_data values(17, 2,  '关闭',     '1',       'sys_notice_status',   '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '关闭状态');
insert into sys_dict_data values(18, 99, '其他',     '0',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '其他操作');
insert into sys_dict_data values(19, 1,  '新增',     '1',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '新增操作');
insert into sys_dict_data values(20, 2,  '修改',     '2',       'sys_oper_type',       '',   'info',    'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '修改操作');
insert into sys_dict_data values(21, 3,  '删除',     '3',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '删除操作');
insert into sys_dict_data values(22, 4,  '授权',     '4',       'sys_oper_type',       '',   'primary', 'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '授权操作');
insert into sys_dict_data values(23, 5,  '导出',     '5',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '导出操作');
insert into sys_dict_data values(24, 6,  '导入',     '6',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '导入操作');
insert into sys_dict_data values(25, 7,  '强退',     '7',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '强退操作');
insert into sys_dict_data values(26, 8,  '生成代码', '8',       'sys_oper_type',       '',   'warning', 'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '生成操作');
insert into sys_dict_data values(27, 9,  '清空数据', '9',       'sys_oper_type',       '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '清空操作');
insert into sys_dict_data values(28, 1,  '成功',     '0',       'sys_common_status',   '',   'primary', 'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '正常状态');
insert into sys_dict_data values(29, 2,  '失败',     '1',       'sys_common_status',   '',   'danger',  'N', '0', 'admin', CURRENT_TIMESTAMP, '', null, '停用状态');


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
create table sys_config (
  config_id         int          not null auto_increment    ,
  config_name       varchar(100)    default ''                 ,
  config_key        varchar(100)    default ''                 ,
  config_value      varchar(500)    default ''                 ,
  config_type       char(1)         default 'N'                ,
  create_by         varchar(64)     default ''                 ,
  create_time       datetime                                   ,
  update_by         varchar(64)     default ''                 ,
  update_time       datetime                                   ,
  remark            varchar(500)    default null               ,
  primary key (config_id)
);

insert into sys_config values(1, '主框架页-默认皮肤样式名称',     'sys.index.skinName',               'skin-blue',     'Y', 'admin', CURRENT_TIMESTAMP, '', null, '蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow' );
insert into sys_config values(2, '用户管理-账号初始密码',         'sys.user.initPassword',            '123456',        'Y', 'admin', CURRENT_TIMESTAMP, '', null, '初始化密码 123456' );
insert into sys_config values(3, '主框架页-侧边栏主题',           'sys.index.sideTheme',              'theme-dark',    'Y', 'admin', CURRENT_TIMESTAMP, '', null, '深色主题theme-dark，浅色主题theme-light' );
insert into sys_config values(4, '账号自助-验证码开关',           'sys.account.captchaEnabled',       'true',          'Y', 'admin', CURRENT_TIMESTAMP, '', null, '是否开启验证码功能（true开启，false关闭）');
insert into sys_config values(5, '账号自助-是否开启用户注册功能', 'sys.account.registerUser',         'false',         'Y', 'admin', CURRENT_TIMESTAMP, '', null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(6, '用户登录-黑名单列表',           'sys.login.blackIPList',            '',              'Y', 'admin', CURRENT_TIMESTAMP, '', null, '设置登录IP黑名单限制，多个匹配项以;分隔，支持匹配（*通配、网段）');
insert into sys_config values(7, '用户管理-初始密码修改策略',     'sys.account.initPasswordModify',   '1',             'Y', 'admin', CURRENT_TIMESTAMP, '', null, '0：初始密码修改策略关闭，没有任何提示，1：提醒用户，如果未修改初始密码，则在登录时就会提醒修改密码对话框');
insert into sys_config values(8, '用户管理-账号密码更新周期',     'sys.account.passwordValidateDays', '0',             'Y', 'admin', CURRENT_TIMESTAMP, '', null, '密码更新周期（填写数字，数据初始化值为0不限制，若修改必须为大于0小于365的正整数），如果超过这个周期登录系统时，则在登录时就会提醒修改密码对话框');
insert into sys_config values(9, '用户管理-密码字符范围',         'sys.account.chrtype',              '0',             'Y', 'admin', CURRENT_TIMESTAMP, '', null, '默认任意字符范围，0任意（密码可以输入任意字符），1数字（密码只能为0-9数字），2英文字母（密码只能为a-z和A-Z字母），3字母和数字（密码必须包含字母，数字）,4字母数字和特殊字符（目前支持的特殊字符包括：~!@#$%^&*()-=_+）');


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
create table sys_logininfor (
  info_id        bigint     not null auto_increment   ,
  user_name      varchar(50)    default ''                ,
  ipaddr         varchar(128)   default ''                ,
  login_location varchar(255)   default ''                ,
  browser        varchar(50)    default ''                ,
  os             varchar(50)    default ''                ,
  status         char(1)        default '0'               ,
  msg            varchar(255)   default ''                ,
  login_time     datetime                                 ,
  primary key (info_id),
  key idx_sys_logininfor_s  (status),
  key idx_sys_logininfor_lt (login_time)
);


-- ----------------------------
-- 15、定时任务调度表
-- ----------------------------
create table sys_job (
  job_id              bigint    not null auto_increment    ,
  job_name            varchar(64)   default ''                 ,
  job_group           varchar(64)   default 'DEFAULT'          ,
  invoke_target       varchar(500)  not null                   ,
  cron_expression     varchar(255)  default ''                 ,
  misfire_policy      varchar(20)   default '3'                ,
  concurrent          char(1)       default '1'                ,
  status              char(1)       default '0'                ,
  create_by           varchar(64)   default ''                 ,
  create_time         datetime                                 ,
  update_by           varchar(64)   default ''                 ,
  update_time         datetime                                 ,
  remark              varchar(500)  default ''                 ,
  primary key (job_id, job_name, job_group)
);

insert into sys_job values(1, '系统默认（无参）', 'DEFAULT', 'ryTask.ryNoParams',        '0/10 * * * * ?', '3', '1', '1', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_job values(2, '系统默认（有参）', 'DEFAULT', 'ryTask.ryParams(''ry'')',  '0/15 * * * * ?', '3', '1', '1', 'admin', CURRENT_TIMESTAMP, '', null, '');
insert into sys_job values(3, '系统默认（多参）', 'DEFAULT', 'ryTask.ryMultipleParams(''ry'', true, 2000L, 316.50D, 100)',  '0/20 * * * * ?', '3', '1', '1', 'admin', CURRENT_TIMESTAMP, '', null, '');


-- ----------------------------
-- 16、定时任务调度日志表
-- ----------------------------
create table sys_job_log (
  job_log_id          bigint     not null auto_increment    ,
  job_name            varchar(64)    not null                   ,
  job_group           varchar(64)    not null                   ,
  invoke_target       varchar(500)   not null                   ,
  job_message         varchar(500)                              ,
  status              char(1)        default '0'                ,
  exception_info      varchar(2000)  default ''                 ,
  start_time          datetime                                  ,
  end_time            datetime                                  ,
  create_time         datetime                                  ,
  primary key (job_log_id)
);


-- ----------------------------
-- 17、通知公告表
-- ----------------------------
create table sys_notice (
  notice_id         int          not null auto_increment    ,
  notice_title      varchar(50)     not null                   ,
  notice_type       char(1)         not null                   ,
  notice_content    longblob        default null               ,
  status            char(1)         default '0'                ,
  create_by         varchar(64)     default ''                 ,
  create_time       datetime                                   ,
  update_by         varchar(64)     default ''                 ,
  update_time       datetime                                   ,
  remark            varchar(255)    default null               ,
  primary key (notice_id)
);

-- ----------------------------
-- 初始化-公告信息表数据
-- ----------------------------
insert into sys_notice values('1', '温馨提醒：2018-07-01 若依新版本发布啦', '2', '新版本内容', '0', 'admin', CURRENT_TIMESTAMP, '', null, '管理员');
insert into sys_notice values('2', '维护通知：2018-07-01 若依系统凌晨维护', '1', '维护内容',   '0', 'admin', CURRENT_TIMESTAMP, '', null, '管理员');
insert into sys_notice values('3', '若依开源框架介绍', '1', '<p><span style=\"color: rgb(230, 0, 0);\">项目介绍</span></p><p><font color=\"#333333\">RuoYi开源项目是为企业用户定制的后台脚手架框架，为企业打造的一站式解决方案，降低企业开发成本，提升开发效率。主要包括用户管理、角色管理、部门管理、菜单管理、参数管理、字典管理、</font><span style=\"color: rgb(51, 51, 51);\">岗位管理</span><span style=\"color: rgb(51, 51, 51);\">、定时任务</span><span style=\"color: rgb(51, 51, 51);\">、</span><span style=\"color: rgb(51, 51, 51);\">服务监控、登录日志、操作日志、代码生成等功能。其中，还支持多数据源、数据权限、国际化、Redis缓存、Docker部署、滑动验证码、第三方认证登录、分布式事务、</span><font color=\"#333333\">分布式文件存储</font><span style=\"color: rgb(51, 51, 51);\">、分库分表处理等技术特点。</span></p><p><img src=\"https://foruda.gitee.com/images/1773931848342439032/a4d22313_1815095.png\" style=\"width: 64px;\"><br></p><p><span style=\"color: rgb(230, 0, 0);\">官网及演示</span></p><p><span style=\"color: rgb(51, 51, 51);\">若依官网地址：&nbsp;</span><a href=\"http://ruoyi.vip\" target=\"_blank\">http://ruoyi.vip</a><a href=\"http://ruoyi.vip\" target=\"_blank\"></a></p><p><span style=\"color: rgb(51, 51, 51);\">若依文档地址：&nbsp;</span><a href=\"http://doc.ruoyi.vip\" target=\"_blank\">http://doc.ruoyi.vip</a><br></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【不分离版】：&nbsp;</span><a href=\"http://demo.ruoyi.vip\" target=\"_blank\">http://demo.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【分离版本】：&nbsp;</span><a href=\"http://vue.ruoyi.vip\" target=\"_blank\">http://vue.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【微服务版】：&nbsp;</span><a href=\"http://cloud.ruoyi.vip\" target=\"_blank\">http://cloud.ruoyi.vip</a></p><p><span style=\"color: rgb(51, 51, 51);\">演示地址【移动端版】：&nbsp;</span><a href=\"http://h5.ruoyi.vip\" target=\"_blank\">http://h5.ruoyi.vip</a></p><p><br style=\"color: rgb(48, 49, 51); font-family: &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 12px;\"></p>', '0', 'admin', CURRENT_TIMESTAMP, '', null, '管理员');


-- ----------------------------
-- 18、公告已读记录表
-- ----------------------------
create table sys_notice_read (
  read_id          bigint       not null auto_increment    ,
  notice_id        int           not null                   ,
  user_id          bigint       not null                   ,
  read_time        datetime         not null                   ,
  primary key (read_id),
  unique key uk_user_notice (user_id, notice_id)   
);


-- ----------------------------
-- 19、代码生成业务表
-- ----------------------------
create table gen_table (
  table_id          bigint      not null auto_increment    ,
  table_name        varchar(200)    default ''                 ,
  table_comment     varchar(500)    default ''                 ,
  sub_table_name    varchar(64)     default null               ,
  sub_table_fk_name varchar(64)     default null               ,
  class_name        varchar(100)    default ''                 ,
  tpl_category      varchar(200)    default 'crud'             ,
  tpl_web_type      varchar(30)     default ''                 ,
  package_name      varchar(100)                               ,
  module_name       varchar(30)                                ,
  business_name     varchar(30)                                ,
  function_name     varchar(50)                                ,
  function_author   varchar(50)                                ,
  form_col_num      int          default 1                  ,
  gen_type          char(1)         default '0'                ,
  gen_path          varchar(200)    default '/'                ,
  options           varchar(1000)                              ,
  create_by         varchar(64)     default ''                 ,
  create_time 	    datetime                                   ,
  update_by         varchar(64)     default ''                 ,
  update_time       datetime                                   ,
  remark            varchar(500)    default null               ,
  primary key (table_id)
);


-- ----------------------------
-- 20、代码生成业务表字段
-- ----------------------------
create table gen_table_column (
  column_id         bigint      not null auto_increment    ,
  table_id          bigint                                 ,
  column_name       varchar(200)                               ,
  column_comment    varchar(500)                               ,
  column_type       varchar(100)                               ,
  java_type         varchar(500)                               ,
  java_field        varchar(200)                               ,
  is_pk             char(1)                                    ,
  is_increment      char(1)                                    ,
  is_required       char(1)                                    ,
  is_insert         char(1)                                    ,
  is_edit           char(1)                                    ,
  is_list           char(1)                                    ,
  is_query          char(1)                                    ,
  query_type        varchar(200)    default 'EQ'               ,
  html_type         varchar(200)                               ,
  dict_type         varchar(200)    default ''                 ,
  sort              int                                        ,
  create_by         varchar(64)     default ''                 ,
  create_time 	    datetime                                   ,
  update_by         varchar(64)     default ''                 ,
  update_time       datetime                                   ,
  primary key (column_id)
);
ALTER TABLE sys_user ADD CONSTRAINT uk_edu_username UNIQUE(user_name);
ALTER TABLE sys_user ADD CONSTRAINT uk_edu_phone UNIQUE(phonenumber);

-- Education phase 1. UTC datetime. Business identity keys remain reserved after logical deletion.
CREATE TABLE biz_schema_version (
version_id bigint PRIMARY KEY, version_name varchar(128) NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0
);

CREATE TABLE biz_org_extend (
dept_id bigint PRIMARY KEY, org_type varchar(16) NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(dept_id) REFERENCES sys_dept(dept_id)
);

CREATE TABLE biz_school (
school_id bigint PRIMARY KEY, school_code varchar(32) NOT NULL UNIQUE, school_name varchar(100) NOT NULL, expire_time datetime NOT NULL, school_status varchar(16) NOT NULL DEFAULT 'ACTIVE',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES sys_dept(dept_id), INDEX ix_school_expiry(expire_time)
);

CREATE TABLE biz_class (
class_id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NOT NULL, academic_year varchar(9) NOT NULL, grade tinyint NOT NULL, class_name varchar(64) NOT NULL, status varchar(16) NOT NULL DEFAULT 'ACTIVE',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES biz_school(school_id), UNIQUE(school_id,academic_year,grade,class_name), CHECK(grade BETWEEN 1 AND 6)
);

CREATE TABLE biz_account_security (
user_id bigint PRIMARY KEY, password_version int NOT NULL DEFAULT 1, must_change_pwd tinyint NOT NULL DEFAULT 1, phone_verified tinyint NOT NULL DEFAULT 0, verified_phone varchar(11) NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(user_id) REFERENCES sys_user(user_id)
);

CREATE TABLE biz_student_extend (
user_id bigint PRIMARY KEY, school_id bigint NOT NULL, class_id bigint NOT NULL, student_no varchar(32) NOT NULL, is_trial tinyint NOT NULL DEFAULT 0,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(user_id) REFERENCES sys_user(user_id), FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(class_id) REFERENCES biz_class(class_id), UNIQUE(school_id,student_no), INDEX ix_student_class(school_id,class_id,del_flag)
);

CREATE TABLE biz_teacher_class (
id bigint PRIMARY KEY AUTO_INCREMENT, teacher_user_id bigint NOT NULL, school_id bigint NOT NULL, class_id bigint NOT NULL, subject_code varchar(16) NOT NULL DEFAULT 'ANY', start_time datetime NOT NULL, end_time datetime NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(teacher_user_id) REFERENCES sys_user(user_id), FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(class_id) REFERENCES biz_class(class_id), INDEX ix_teaching(teacher_user_id,class_id,end_time)
);

CREATE TABLE biz_student_class_history (
id bigint PRIMARY KEY AUTO_INCREMENT, student_user_id bigint NOT NULL, school_id bigint NOT NULL, class_id bigint NOT NULL, start_time datetime NOT NULL, end_time datetime NULL, change_reason varchar(500) NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(student_user_id) REFERENCES sys_user(user_id), FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(class_id) REFERENCES biz_class(class_id), INDEX ix_history(student_user_id,start_time)
);

CREATE TABLE biz_game (
game_id bigint PRIMARY KEY AUTO_INCREMENT, game_code varchar(64) NOT NULL UNIQUE, game_name varchar(128) NOT NULL,
category_code varchar(32) NOT NULL DEFAULT 'MATH', source_type varchar(16) NOT NULL, entry_path varchar(1024) NOT NULL,
access_policy varchar(32) NOT NULL DEFAULT 'TEACHER_ONLY', status varchar(16) NOT NULL DEFAULT 'DRAFT',
version_no varchar(32) NOT NULL DEFAULT 'v1', description varchar(4000) NOT NULL DEFAULT '', sort_no int NOT NULL DEFAULT 0,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
INDEX ix_game_catalog(status,category_code,sort_no), CHECK(source_type IN ('INTERNAL','EXTERNAL')), CHECK(access_policy IN ('TEACHER_ONLY','STUDENT','COURSEWARE'))
);

CREATE TABLE biz_courseware_base (
courseware_id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NULL, subject_code varchar(16) NOT NULL,
volume tinyint NOT NULL, stage_code varchar(16) NOT NULL DEFAULT 'BASIC', courseware_name varchar(128) NOT NULL,
description varchar(4000) NOT NULL DEFAULT '', status varchar(16) NOT NULL DEFAULT 'DRAFT', sort_no int NOT NULL DEFAULT 0,
version_no int NOT NULL DEFAULT 0, source_courseware_id bigint NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(source_courseware_id) REFERENCES biz_courseware_base(courseware_id), INDEX ix_courseware(school_id,subject_code,volume,status), CHECK(volume BETWEEN 1 AND 12)
);

CREATE TABLE biz_courseware_version (
id bigint PRIMARY KEY AUTO_INCREMENT, courseware_id bigint NOT NULL, version_no int NOT NULL, content_snapshot_json json NOT NULL, publisher varchar(64) NOT NULL, publish_time datetime NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), UNIQUE(courseware_id,version_no)
);

CREATE TABLE biz_courseware_game (
id bigint PRIMARY KEY AUTO_INCREMENT, courseware_id bigint NOT NULL, game_id bigint NOT NULL, sort_no int NOT NULL DEFAULT 0, game_mode varchar(16) NOT NULL DEFAULT 'TRAINING',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), FOREIGN KEY(game_id) REFERENCES biz_game(game_id), UNIQUE(courseware_id,game_id)
);

CREATE TABLE biz_school_resource (
resource_id bigint PRIMARY KEY AUTO_INCREMENT, school_id bigint NOT NULL, subject_code varchar(16) NULL, volume tinyint NULL, courseware_id bigint NULL, start_time datetime NOT NULL, end_time datetime NOT NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(school_id) REFERENCES biz_school(school_id), FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), INDEX ix_pool(school_id,start_time,end_time), CHECK(end_time>start_time)
);

CREATE TABLE biz_student_entitlement (
entitlement_id bigint PRIMARY KEY AUTO_INCREMENT, student_user_id bigint NOT NULL, source_type varchar(16) NOT NULL, source_id bigint NULL, start_time datetime NOT NULL, end_time datetime NOT NULL, status varchar(16) NOT NULL DEFAULT 'ACTIVE',
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(student_user_id) REFERENCES sys_user(user_id), INDEX ix_entitlement(student_user_id,status,end_time), CHECK(end_time>start_time)
);

CREATE TABLE biz_entitlement_scope (
scope_id bigint PRIMARY KEY AUTO_INCREMENT, entitlement_id bigint NOT NULL, subject_code varchar(16) NULL, volume tinyint NULL, stage_code varchar(16) NULL, courseware_id bigint NULL, game_id bigint NULL,
create_by varchar(64) NOT NULL DEFAULT '', create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
update_by varchar(64) NOT NULL DEFAULT '', update_time datetime NULL, remark varchar(500) NOT NULL DEFAULT '',
del_flag char(1) NOT NULL DEFAULT '0', row_version bigint NOT NULL DEFAULT 0 ,
FOREIGN KEY(entitlement_id) REFERENCES biz_student_entitlement(entitlement_id), FOREIGN KEY(courseware_id) REFERENCES biz_courseware_base(courseware_id), FOREIGN KEY(game_id) REFERENCES biz_game(game_id)
);

INSERT INTO biz_schema_version(version_id,version_name) VALUES(2,'education-phase1');

INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5000,'教学资源',0,0,'education',NULL,'M','','education',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5010,'游戏管理',5000,1,'games','education/games/index','C','biz:game:list','example',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5020,'课件管理',5000,2,'courseware','education/courseware/index','C','biz:courseware:list','documentation',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5030,'用户管理',5000,3,'users','education/users/index','C','biz:user:list','user',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5040,'学校管理',5000,4,'schools','education/schools/index','C','biz:school:list','tree',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5050,'班级管理',5000,5,'classes','education/classes/index','C','biz:class:list','peoples',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5060,'学生授权',5000,6,'entitlements','education/entitlements/index','C','biz:entitlement:list','lock',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5070,'学校资源池',5000,7,'pool','education/pool/index','C','biz:pool:list','list',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5101,'query',5010,5101,'#',NULL,'F','biz:game:query','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5102,'add',5010,5102,'#',NULL,'F','biz:game:add','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5103,'edit',5010,5103,'#',NULL,'F','biz:game:edit','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5104,'remove',5010,5104,'#',NULL,'F','biz:game:remove','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5105,'preview',5010,5105,'#',NULL,'F','biz:game:preview','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5106,'query',5020,5106,'#',NULL,'F','biz:courseware:query','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5107,'add',5020,5107,'#',NULL,'F','biz:courseware:add','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5108,'edit',5020,5108,'#',NULL,'F','biz:courseware:edit','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5109,'remove',5020,5109,'#',NULL,'F','biz:courseware:remove','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5110,'preview',5020,5110,'#',NULL,'F','biz:courseware:preview','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5111,'query',5030,5111,'#',NULL,'F','biz:user:query','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5112,'add',5030,5112,'#',NULL,'F','biz:user:add','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5113,'edit',5030,5113,'#',NULL,'F','biz:user:edit','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5114,'remove',5030,5114,'#',NULL,'F','biz:user:remove','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5115,'resetPwd',5030,5115,'#',NULL,'F','biz:user:resetPwd','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5116,'query',5040,5116,'#',NULL,'F','biz:school:query','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5117,'add',5040,5117,'#',NULL,'F','biz:school:add','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5118,'edit',5040,5118,'#',NULL,'F','biz:school:edit','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5119,'remove',5040,5119,'#',NULL,'F','biz:school:remove','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5120,'query',5050,5120,'#',NULL,'F','biz:class:query','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5121,'add',5050,5121,'#',NULL,'F','biz:class:add','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5122,'edit',5050,5122,'#',NULL,'F','biz:class:edit','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5123,'remove',5050,5123,'#',NULL,'F','biz:class:remove','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5124,'query',5060,5124,'#',NULL,'F','biz:entitlement:query','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5125,'add',5060,5125,'#',NULL,'F','biz:entitlement:add','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5126,'edit',5060,5126,'#',NULL,'F','biz:entitlement:edit','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5127,'remove',5060,5127,'#',NULL,'F','biz:entitlement:remove','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5128,'grant',5060,5128,'#',NULL,'F','biz:entitlement:grant','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5129,'query',5070,5129,'#',NULL,'F','biz:pool:query','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5130,'add',5070,5130,'#',NULL,'F','biz:pool:add','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5131,'edit',5070,5131,'#',NULL,'F','biz:pool:edit','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_menu(menu_id,menu_name,parent_id,order_num,path,component,menu_type,perms,icon,is_frame,is_cache,visible,status,create_by,create_time) VALUES(5132,'remove',5070,5132,'#',NULL,'F','biz:pool:remove','#',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time) VALUES(101,'学校管理员','edu_school',101,'3',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time) VALUES(102,'老师','edu_teacher',102,'5',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time) VALUES(103,'学生','edu_student',103,'5',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time) VALUES(104,'区域管理员','edu_region',104,'4',1,1,'0','0','migration',CURRENT_TIMESTAMP);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5000);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5010);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5020);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5030);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5040);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5050);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5060);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5070);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5105);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5106);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5107);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5108);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5110);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5111);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5112);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5113);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5115);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5116);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5118);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5120);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5121);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5122);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5128);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5129);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5130);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5131);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(101,5132);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(102,5000);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(102,5010);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(102,5020);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(102,5030);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(102,5105);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(102,5110);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(102,5111);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(103,5000);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(103,5010);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(103,5020);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(103,5105);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(103,5110);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5000);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5010);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5020);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5030);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5040);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5050);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5060);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5070);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5105);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5106);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5107);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5108);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5110);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5111);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5112);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5113);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5115);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5116);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5118);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5120);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5121);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5122);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5128);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5129);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5130);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5131);
INSERT INTO sys_role_menu(role_id,menu_id) VALUES(104,5132);
INSERT INTO biz_game(game_id,game_code,game_name,category_code,source_type,entry_path,access_policy,status,version_no,description) VALUES(1,'planet','星球探索','MATH','INTERNAL','index.html','TEACHER_ONLY','PUBLISHED','v1','星球教学资源入口，仅老师及管理员可用。');
INSERT INTO biz_game(game_id,game_code,game_name,category_code,source_type,entry_path,access_policy,status,version_no,description) VALUES(2,'number-match','数字配对','MATH','INTERNAL','index.html','STUDENT','PUBLISHED','v1','适合低年级的数字配对演示，需要单独授权。');
