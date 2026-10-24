# 部署与迁移

## 统一域名

后台和可信内部游戏统一使用 `https://fengyucode.zhangff.cn`：

| 内容 | 地址 |
|---|---|
| 教育后台 | `https://fengyucode.zhangff.cn/` |
| 星球游戏 | `https://fengyucode.zhangff.cn/play/planet/index.html` |
| 数字配对 | `https://fengyucode.zhangff.cn/play/number-match/index.html` |
| 管理 API | `https://fengyucode.zhangff.cn/prod-api/` |

网站网关将 `/prod-api/` 和 `/play/` 代理到 API；`/play/` 不提供公开文件目录。游戏 HTML、脚本及素材逐请求检查游戏会话、父登录会话、账号、学校、角色权限和授权。学生直访未授权游戏返回403，老师通过菜单预览取得单次票据再进入。

同源下游戏代码与后台共享浏览器安全边界，路径不能提供来源隔离。内部目录仅部署可信游戏；外部游戏继续使用 HTTPS 白名单外链。游戏权限校验依然由后端负责，不因同域省略。

## 当前演示部署（不需要 Redis / MySQL）

在 `education-admin` 目录执行：

```sh
cp deploy/demo.env.example deploy/.env.demo
# 编辑 deploy/.env.demo：设置演示密码，并使用 openssl rand -hex 32 生成 EDU_TOKEN_SECRET
docker compose --env-file deploy/.env.demo -f deploy/compose.demo.yaml up -d --build
```

本机访问 `http://localhost:18090`，游戏使用相同地址下的 `/play/`。镜像构建现有星球游戏并打入 API，无需在服务器安装 Node.js。默认保留验证码和首次强制改密。

部署到现有 HTTPS 服务器时，在 `deploy/.env.demo` 设置：

```dotenv
EDU_ADMIN_ORIGIN=https://fengyucode.zhangff.cn
EDU_GAME_ORIGIN=https://fengyucode.zhangff.cn
EDU_SECURE_GAME_COOKIE=true
```

域名只需代理到本机18090端口。Caddy示例：

```caddy
fengyucode.zhangff.cn {
    reverse_proxy 127.0.0.1:18090
}
```

该配置只有 API 和网站网关两个服务，使用 dev 样例数据、H2 内存库和本地缓存，只运行一个 API 实例。后端重启会清空编辑内容、密码变更和会话，再生成示范学校、班级、老师、学生与授权。演示账号密码通过环境变量设置；Redis 专用监控不启用。

`education.yml` 包含无 Redis / MySQL 的演示容器构建启动与接口验收任务，检查统一网站入口下匿名游戏访问返回403。正常部署不关闭验证码；CI只对一次性测试容器显式关闭验证码。本机未运行 Docker daemon，容器启动与线上结果仍须在对应环境验证。

## 正式数据库与缓存预留

`dev` 固定使用内存缓存，排除 Redis 自动配置，不尝试连接 Redis。正式 `education` 配置默认使用 Redis，保留 RedisTemplate 和连接环境变量。验证码、登录、限流、游戏票据和会话使用统一缓存门面。

以下 `deploy/compose.yaml` 保留正式 MySQL + Redis 方案，当前演示无需执行：

1. 复制 `.env.example` 为 `.env`，替换密码和Token密钥。后台和游戏来源默认均为 `https://fengyucode.zhangff.cn`。
2. `./scripts/build-games.sh` 构建星球版本，版本目录以只读卷挂载。
3. `docker compose --env-file .env -f deploy/compose.yaml up -d --build`。首次空 MySQL 数据卷按 V001至V003初始化。已有库应执行评审后的增量SQL，禁止删除卷以“升级”。
4. 将域名代理到18090。API、MySQL和Redis不映射公网端口。
5. admin 首次登录强制改密，再登记学校有效期、班级、资源池及用户。

正式环境保持 HTTPS 与 Secure 游戏Cookie。外部游戏须在 `EDU_EXTERNAL_HOSTS` 列出精确HTTPS主机名。真实客户IP的代理信任链按服务器统一配置，只信任受控代理。

## 原星球站点迁移

原 `fengyucode.zhangff.cn` 根路径展示星球游戏；切换后根路径展示教育后台，星球入口变为 `/play/planet/`。部署目录建议 `/project/education-admin`，不覆盖原项目文件或数据卷；确认新服务可用后，再把现有域名反向代理切换到18090，保留旧配置以便回滚。

原星球生产代码会检查游戏会话，旧Nginx也会鉴权，不应继续保留匿名文件备用入口。旧独立容器未接入API时会拒绝游戏资源。`deploy/nginx.games.conf` 仅保留可选独立游戏网关参考，默认Compose不再启用它。

原星球本地纯UI调试可使用 `VITE_STANDALONE_DEV=true npm run dev`，仅限开发模式，生产构建不接受该绕过开关。

## 更新与回滚

游戏按不可变版本目录发布，先验证新目录，再修改版本号；回滚指向已有版本，不覆盖相同版本文件。课件发布保留快照，过期编辑以 row_version 返回409。

MySQL DDL不是可完整回滚的普通事务。执行前备份数据库并在同版本测试库验证，V001至V003仅用于新库，后续使用新编号迁移。API启动不覆盖已有管理员密码。

本次代码修改不包含服务器发布、域名代理切换或创建生产数据库，不代表现有线上站点已经变更。
