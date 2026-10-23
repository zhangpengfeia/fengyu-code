# 部署与迁移

## 独立部署

本项目没有修改原星球项目的Compose项目名、镜像身份、服务器目录或GitHub Secrets。建议新目录 `/project/education-admin`、独立域名与独立数据卷。示例网关只绑定127.0.0.1的18090和18091端口，外部HTTPS由现有Caddy或受信任反向代理提供。

1. 复制`.env.example`为`.env`，替换所有密码和Token签名密钥。
2. 指定`EDU_ADMIN_ORIGIN=https://admin.example.com`及`EDU_GAME_ORIGIN=https://games.example.com`，管理端使用另一个主机名如`https://admin.example.com`。**不同端口不足以隔离Cookie**，不要把游戏部署在管理端主机名下。
3. `./scripts/build-games.sh`构建星球版本。游戏版本目录以只读卷挂载，API不接收任意HTML游戏包上传。
4. `docker compose --env-file .env -f deploy/compose.yaml up -d --build`。仅在首次空MySQL数据卷时，镜像按V001至V003顺序初始化数据库。已有数据库应通过评审后的增量SQL升级，禁止删除卷以“升级”。
5. HTTPS管理域名代理到127.0.0.1:18090；游戏域名代理到127.0.0.1:18091。游戏网关仅代理`/play/`，不暴露原始文件目录、后台页面或管理API。后台没有公网映射端口。
6. 以设置的初始admin密码登录并完成强制改密；先在原生部门管理建立学校节点，再在学校管理登记业务信息和有效期，建立班级、学校资源池及用户。

生产默认启用Secure游戏Cookie。只有本地HTTP开发时允许关闭。外部引用须在`EDU_EXTERNAL_HOSTS`以逗号列出HTTPS精确主机名；不允许任意跳转或把内部游戏伪装成公开外链。

Caddy配置示例（替换域名）：

```caddy
admin.example.com {
    reverse_proxy 127.0.0.1:18090
}
games.example.com {
    reverse_proxy 127.0.0.1:18091
}
```

真实客户IP的代理信任链按现有部署统一配置，只信任受控代理。数据库和Redis没有公网端口。Dockerfile与Compose已经提供，但当前机器Docker daemon未运行，因此本次未实际执行容器部署，也未连接生产MySQL。

## 原星球公开入口

`gamesoptionproject/src/main.ts`生产构建会检查游戏会话；原Nginx配置新增auth_request，HTML和静态资源失败关闭并展示403。健康检查和发布标记继续可用。

原Nginx如继续保留，需要和`education-api`处在可解析的受控Docker网络；未接入时拒绝资源访问。这是有意关闭匿名备用入口。推荐让平台通过独立游戏域名的`/play/planet/`提供服务，并撤下旧匿名站点；不要把旧站继续当公开文件CDN。

原星球本地纯UI调试可显式使用`VITE_STANDALONE_DEV=true npm run dev`，只在Vite开发模式生效；生产编译不会接受这个绕过开关。

## 更新与回滚

游戏按不可变版本目录发布，先上传与验证新目录，再修改目录版本。回滚将目录条目版本指回已有构建；不要覆盖相同版本文件。课件发布生成历史快照，过期编辑通过row_version返回409。

数据库DDL在MySQL中不是可完整回滚的普通事务。执行SQL前备份数据库、保留文件版本和当前镜像标识，先在同版本MySQL测试库执行。当前V001–V003用于新库，后续变更必须新增编号迁移。API启动不会自动覆盖既有管理员密码。

备份`education-db`和资源文件；恢复演练与容量测试需在实际部署环境执行。本次没有向服务器推送代码、修改DNS、创建生产数据库或发布线上站点。
