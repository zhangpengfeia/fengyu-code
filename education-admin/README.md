# 智慧教育平台

在原第一期实现上，按用户提供的V1.0需求增加配音、课堂PK、积分、激活码、学生注册和学科扩展。实施范围、数据库升级及尚未覆盖的细节见 [V1.0实施说明](docs/REQUIREMENTS_V1_IMPLEMENTATION.md)。使用官方 RuoYi Vue3 + Element Plus 和 Spring Boot 3 + MyBatis + Maven，前后端独立目录。账号、角色、菜单、权限指令和日志复用若依，教育业务在独立模块扩展。

## 本期功能

- 账号、已验证手机号、学校＋学号三种密码登录；验证码、失败锁定、随机初始密码和首次强制改密。
- 老师、学生、学校管理员、区域管理员及超级管理员的菜单权限；后端每次请求重新读取账号与角色状态。
- 用户查询、创建、修改、冻结、密码重置；学生班级与历史归属、老师任教关系；数据库唯一约束与事务回滚。
- 学校有效期、人工停用、班级与学年管理；学校资源池与学生单课件或单游戏手动授权、撤销。
- 课件学科、册次、阶次、游戏关联、发布快照与版本查询；资源编辑使用乐观锁避免过期页面覆盖。
- 游戏分类、分页搜索、内部/外部接入、访问策略、发布校验和预览；已接入星球项目与数字配对演示。
- 内部游戏采用60秒单次票据、独立游戏会话、受保护HTML和静态资源，逐请求校验权限、学校有效期、密码和父登录会话。旧链接、学生直访、撤销授权及密码重置均不能绕过。

## 目录

```text
education-admin/
├── frontend/                   # 官方若依 Vue3 与教育业务页面
├── backend/
│   ├── ruoyi-education/        # 教育业务、权限、游戏网关及测试
│   ├── ruoyi-admin/            # Spring Boot 启动入口
│   └── ruoyi-*/                # 保留的若依原生模块
├── sql/                       # MySQL 8.0 建表和角色菜单初始化
├── games/
│   ├── planet/manifest.json   # ../gamesoptionproject 构建接入
│   └── number-match/v1/       # 可运行配对游戏示例
├── scripts/                   # 游戏打包、本地启动、验收脚本
├── deploy/                    # Dockerfile、Compose、两个网关
└── docs/                      # API、验收范围与部署说明
```

官方版本和必要框架接入改动见 [UPSTREAM.md](UPSTREAM.md)。原始需求文档在仓库根目录 `docs/`。

## 本地开发

需要 JDK 17、Node.js 20.19+ 或 22。**当前开发和演示默认不需要 Redis 或 MySQL**：缓存使用单进程内存实现，业务数据使用 H2。Redis 接口及生产配置保留。未安装 Maven 时 `backend/mvnw` 可下载校验后的 Maven 3.9.9。H2 为一次性内存库，启动时按 MySQL 迁移生成的开发脚本初始化，停机即清空，不会创建或连接生产 MySQL。

最快启动演示后端（内置 `admin / admin123456`，老师、学生、学校管理员密码均为 `DemoUser!2026`）：

```sh
cd education-admin
./scripts/build-games.sh
./scripts/run-demo.sh
```

另开终端执行下方前端启动命令，访问 `http://localhost:5173`。dev 测试管理员可直接使用上述密码登录；正式环境和新建业务账号仍须首次改密。默认验证码保留。你之前启动的旧版后端需要先按 Ctrl+C 停止，再使用新脚本启动；Redis 进程不再需要。

如需自行设置演示密码，使用以下完整步骤：

1. 进入项目目录并打包游戏：

```sh
./scripts/build-games.sh
```

2. 启动后端。密码和密钥由你设置，下面为环境变量说明，不提供生产默认密码：

```sh
export EDU_ADMIN_PASSWORD='你设置的初始管理员密码'
export EDU_TOKEN_SECRET='至少32字符的随机签名密钥'
# 可选：仅dev模式创建 teacher/student/schooladmin 演示账号，使用你指定的演示密码
export EDU_DEMO_PASSWORD='你设置的开发演示密码'
./scripts/run-dev.sh
```

首次管理员账号为 `admin`；dev 测试环境可直接登录，正式环境首次登录必须修改初始密码。演示学生学校ID为11000、学号0001；仅开启 dev 演示数据时，手机号13800000001与13800000002分别用于老师、学生的模拟已验证号码，不用于生产。

3. 在另一终端启动前端：

```sh
cd frontend
npm ci
npm run dev
```

浏览器访问 [http://localhost:5173](http://localhost:5173)。API 为8081。游戏预览也使用 `http://localhost:5173/play/游戏编码/`，由 Vite 代理到后端鉴权；后台与可信内部游戏使用同一域名。

正常开发默认保留验证码。自动UI测试如需关闭，仅在dev环境显式添加 `--education.dev-captcha-enabled=false`，不得用于生产。dev模拟短信码会显示在账号安全页；生产未配置真实短信适配器时拒绝发送和验证，不把管理员录入的未验证号码当成手机号登录凭据。

学校专属登录入口：`/school/DEMO/login`，自动锁定学校。普通学校学号模式填写学校ID。手机号使用独立登录模式，不与纯数字学号混淆。

## 验证

```sh
cd backend
./mvnw -B -ntp test package
cd ../frontend
npm ci
npm run build
cd ..
./scripts/build-games.sh
```

端到端脚本会修改并撤销**一次性 dev 数据**，只允许 loopback 目标且必须显式授权开发模式。内存模式下，先用测试参数启动一个全新的后端（关闭验证码只限本次自动验收；正常演示不要加此参数）：

```sh
./scripts/run-demo.sh --education.dev-captcha-enabled=false
```

另一终端执行：

```sh
EDU_TEST_ADMIN_PASSWORD='admin123456' \
EDU_TEST_DEMO_PASSWORD='DemoUser!2026' \
python3 scripts/smoke-test.py --allow-development
```

Redis 模式仍可传 `--redis-cli /路径/redis-cli --redis-port 6379` 从受控本地 Redis 读取测试验证码，不要指向生产。

该脚本会修改初始管理员密码、重置老师密码、撤销示例游戏授权并使示例学校到期。运行后重启H2开发后端恢复演示数据。结果与验证边界见 [docs/VALIDATION.md](docs/VALIDATION.md)。

## 无外部数据库的演示部署

提供 `deploy/compose.demo.yaml`，仅启动 API 和统一网站网关两个服务，游戏在镜像中构建，无需服务器安装 Node.js、Redis 或 MySQL。需 Docker Compose。

```sh
cd education-admin
cp deploy/demo.env.example deploy/.env.demo
# 将 deploy/.env.demo 中 EDU_TOKEN_SECRET 替换为 openssl rand -hex 32 的输出
# 公网演示将后台和游戏来源均设为 https://fengyucode.zhangff.cn，详见部署说明

docker compose --env-file deploy/.env.demo -f deploy/compose.demo.yaml up -d --build
```

本机访问 `http://localhost:18090`，游戏自动跳转至同站点的 `/play/游戏编码/`。演示数据和会话在后端重启后清空并重新生成，只运行一个 API 实例。它是当前联调、演示用部署；正式业务保留 MySQL + Redis 部署配置。Redis 专用缓存监控在内存模式下不启用。

## 域名与游戏路径

目标部署域名统一为 `https://fengyucode.zhangff.cn`：后台在 `/`，星球游戏在 `/play/planet/index.html`，其他内部游戏在 `/play/游戏编码/`。配置 `EDU_ADMIN_ORIGIN` 和 `EDU_GAME_ORIGIN` 为相同来源。游戏链接与全部静态资源继续由后端鉴权，学生访问未授权游戏返回 403。

同源游戏代码与后台共享浏览器安全边界，内部目录仅部署可信代码；外部第三方游戏继续使用白名单外链，不把未经审核的第三方 HTML 复制到内部目录。原域名根路径的星球游戏入口会由后台替代，部署前需要切换原反向代理；当前代码修改并不代表线上站点已切换。

## MySQL 与部署

当前交付 SQL，未替你建立生产数据库。MySQL空库按顺序执行：

1. `sql/V001__ruoyi_baseline.sql`：官方框架表和基础配置，已移除DROP语句与共享密码账号。
2. `sql/V002__education_schema.sql`：教育业务表、唯一约束、关联约束、索引与行版本。
3. `sql/V003__menus_and_catalog.sql`：教育角色、菜单按钮权限与两款游戏目录。
4. `sql/V004__learning_business.sql`：新增教育业务表、参数和扩展字段。
5. `sql/V005__learning_menus.sql`：新增业务菜单、字典、角色权限和定时任务。

这些是**新库初始化脚本**，不要直接导入已有若依生产库，不要重复执行。后续升级新增增量迁移，不覆盖既有文件。原生SQL与业务SQL均保留索引和身份键；生产迁移需先备份并在MySQL 8.0测试库验证。

部署详见 [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)。本机与独立数据库方案保持独立。当前同域名线上演示发布会有意接管原站点的 `fengyucode-web` 身份和18088端口，保留历史版本及回滚指针。

## 本期边界

原第一期安全与管理能力继续保留。本次新增配音、PK、积分、激活码和批量账号能力，具体范围见实施说明。仍需继续完成：

- 学校定制模板升级运营和完整续期运营流程。
- 组织、课件、素材的完整批量导入导出、转校审批和短信生产供应商。
- 私有OSS上传、签名下载和完整对象存储运维。
- 百度真实语音评分供应商适配、自定义评分权重及相关生产配置。

外部游戏目前以HTTPS白名单链接打开，明确标记“仅控制后台入口”；对方没有SSO或票据协议时，不能承诺保护其公网地址。内部游戏受本项目网关实际保护。原星球公开站点也已加入失败关闭的保护配置，迁移部署前须阅读部署说明，避免继续保留匿名备用入口。

## 线上发布

main推送通过 `Education platform build and deploy` 的全部任务后，会发布到 `https://fengyucode.zhangff.cn`，复用现有生产SSH配置及Caddy路由。`demo-deployment` 只是临时验收；必须等 `publish` 和 `deploy` 成功，且HTTPS版本核对通过，才算线上生效。

本地与线上测试管理员统一为 `admin / admin123456`，管理端与学生端共用账号认证；学生端请选择“账号登录”。线上发布会同步管理员密码，其他演示账号密码及Token密钥保存在服务器 `/project/fengyucode/.pipeline/education.env`（0600）。旧星球工作流现在仅检查源码，不再发布覆盖网站。发布与回滚说明见 `docs/DEPLOYMENT.md`。
