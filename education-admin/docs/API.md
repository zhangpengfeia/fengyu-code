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
