# 第一期接口与权限

API认证沿用若依 `Authorization: Bearer <token>`，Token从Redis恢复且每次重新校验账号、角色、学校与密码凭据状态。返回体沿用若依 `code/msg/data`，列表 `code/rows/total`。新增教育接口真实HTTP状态为401、403、409等；前端仍兼容原生若依响应格式。

| 接口 | 方法 | 权限及用途 |
|---|---|---|
| /biz/auth/login | POST | 匿名，mode=ACCOUNT/PHONE/STUDENT；username、password、code、uuid、school_id |
| /captchaImage | GET | 原生验证码，不改写校验流程 |
| /biz/auth/me | GET | 本人资料、初始密码和手机号状态 |
| /biz/auth/password | POST | 本人旧密码验证；修改后旧Bearer和游戏会话失效 |
| /biz/auth/phone/code | POST | 仅dev模拟发送；生产供应商尚未配置时503 |
| /biz/auth/phone/bind | POST | 密码与一次性验证码验证，数据库唯一约束 |
| /biz/auth/school/{schoolCode} | GET | 学校专属入口，仅返回学校ID与名称 |
| /biz/catalog/{kind} | GET/POST | kind=games/courseware/schools/classes/pool；对应biz:模块:list/add |
| /biz/catalog/{kind}/{id} | GET/PUT/DELETE | 编辑详情edit=true须edit权限；PUT带row_version；不越权返回对象 |
| /biz/courseware/{id}/versions | GET | 课件编辑权限与归属校验，查看发布快照 |
| /biz/games/{id}/launch | POST | biz:game:preview且通过当前资源授权；内部返回短期单次票据URL |
| /biz/users | GET/POST | biz:user:list/add；老师仅查有效任教班学生 |
| /biz/users/{id} | GET/PUT | biz:user:query/edit及目标数据范围、身份边界 |
| /biz/users/{id}/reset-password | POST | biz:user:resetPwd；返回一次性随机初始密码，不记入审计响应 |
| /biz/entitlements?student_user_id=... | GET | 本人或有权限管理员查目标范围 |
| /biz/entitlements | POST | biz:entitlement:grant；单游戏或单课件，起止时间与原因必填 |
| /biz/entitlements/{id}/revoke | POST | 授权权限与目标归属，原因必填 |
| /biz/options | GET | 当前权限内学校和班级；学生只返回本人班级 |
| /play/{code}/exchange | GET | 单次票据原子交换，写HttpOnly、SameSite游戏Cookie，303跳转 |
| /play/{code}/session | GET | 游戏Cookie检查，不返回后台Token或原始会话秘密 |
| /play/{code}/** | GET | 每次重新校验父登录状态、密码、学校、角色、授权；禁止目录穿越/符号链接逃逸 |

时间为带时区ISO8601，数据存储按UTC约定。Bigint主键序列化成字符串。列表page默认1、size默认20、最大100；用户列表在数据库完成过滤与分页。

角色菜单与按钮权限通过原生系统角色管理配置。超级管理员使用原生角色、菜单、部门和登录记录界面。学校管理员不得获得system:user:*、system:role:*等全局原生管理权限；其账号管理走biz:user接口并限制只可委派老师和学生角色。

新增用户只接受受控DTO字段：role_key、user_name（老师等）、nick_name、school_id、class_id、student_no、class_ids、phone、status。客户端提交role_id、password、积分或创建者不会直接写入。用户身份不可直接切换；学生当前学校不能通过通用编辑直接跨校变更。未验证手机号与已验证快照不一致时不能手机号登录。

内部发布须已有 `games/{game_code}/{version_no}/index.html` 和匹配的版本manifest。无源码发布上传接口；由受信任构建脚本或运维部署版本目录。游戏不能通过前端回调自行增加正式积分，本期没有启用正式积分回调。

发布健康检查：匿名 `GET /biz/auth/status`，数据库账号初始化前返回503，完成后返回 `status=UP` 与 `release` 标记，不返回账号或密钥。网站 `/healthz` 代理到该接口。

## V1.0 新增业务接口

接口仍使用原生JWT与统一业务异常，业务控制器为`LearningController`、`StudentRegistrationController`和`AccountBatchController`。

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/biz/student-registration/tree` | 可注册学校的部门级联树，匿名 |
| GET | `/biz/student-registration/classes?school_id=` | 可注册班级，匿名 |
| POST | `/biz/student-registration` | 学生注册，固定学生角色并验证验证码 |
| POST | `/biz/users/batch/template` | 账号Excel模板 |
| POST | `/biz/users/batch/import` | multipart字段file、mode，SKIP或OVERWRITE |
| POST | `/biz/users/batch/reset` | ids数组与可选password |
| POST | `/biz/courseware/{id}/customize` | 从全局模板建立指定school_id的草稿 |
| GET/POST | `/biz/learning/dubbing/materials` | 素材列表／新增 |
| PUT/DELETE | `/biz/learning/dubbing/materials/{id}` | 素材修改／逻辑删除 |
| GET | `/biz/learning/dubbing/exam-config` | 册次抽题配置 |
| PUT | `/biz/learning/dubbing/exam-config/{id}` | 仅超级管理员修改 |
| POST | `/biz/learning/exam/draw` | volume抽题，返回exam_ticket |
| POST | `/biz/learning/dubbing/score` | material_id、dubbing_type、audio_url；测评需exam_ticket，课件配音需courseware_id |
| GET | `/biz/learning/records` | 按班级、学生、册次、配音类型查询 |
| POST | `/biz/learning/records/export` | 同范围Excel导出 |
| GET/POST | `/biz/learning/activation/batches` | 激活码批次列表／生成 |
| GET | `/biz/learning/activation/batches/{id}/codes` | 批次码明细，须导出权限 |
| POST | `/biz/learning/activation/batches/{id}/export` | 激活码Excel导出 |
| POST | `/biz/learning/activation/redeem` | 学生用code一次性兑换 |
| GET | `/biz/learning/groups?class_id=` | 当前班级小组及成员 |
| POST | `/biz/learning/groups/regroup` | class_id、size，随机重分并保留历史 |
| POST | `/biz/learning/groups/add` | 新增小组 |
| POST | `/biz/learning/groups/remove` | 删除小组并将成员转入target_group_id |
| POST | `/biz/learning/groups/move` | 手动调整学生组别 |
| GET | `/biz/learning/pk` | class_id、courseware_id、mode、stage查询赛程 |
| POST | `/biz/learning/pk/match` | 同上参数，整体随机匹配 |
| POST | `/biz/learning/pk/{id}/result` | winner_id，null表示撤销 |
| POST | `/biz/learning/pk/clear` | 重置赛程，保留积分 |
| POST | `/biz/learning/scores` | student_id、courseware_id、stage、score、source、event_key |
| POST | `/biz/learning/scores/{id}/reverse` | 撤销手动积分，PK积分由PK接口撤销 |
| POST | `/biz/learning/scores/all-plus` | class_id、courseware_id，全班事务加分 |
| GET | `/biz/learning/scores?student_id=` | 学生积分流水 |
| GET | `/biz/learning/ranking` | 班级课件／册次榜 |
| GET | `/biz/learning/ranking/school` | school_id及可选grade、volume，管理员范围内榜单 |
| POST | `/biz/learning/classes/archive` | 班级ID数组，毕业归档 |
| GET | `/biz/learning/notices` | 本人负责学校的到期提醒 |

课件保存JSON新增`extension`对象；数学包含`stage`、`ability`及媒体字段，阅读包含`dubbing_material_id`、`config_json`。范围授权的`scope_type`支持`SEMESTER`、`RANGE`、`ALL`；`RANGE`的`permission_range`为包含`volumes`、`subjects`、`stages`数组的对象。

没有配置真实`DubbingScorer`时，评分返回503。请求内的评分字段不会被用作可信结果。部署和未覆盖范围见`REQUIREMENTS_V1_IMPLEMENTATION.md`。
