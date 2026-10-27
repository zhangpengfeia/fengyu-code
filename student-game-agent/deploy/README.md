# 学生端与 Agent 发布

流水线为 `.github/workflows/student-agent.yml`：拉取请求执行测试与构建；推送 `main` 或手动运行时，发布两个不可变版本镜像并使用已有 SSH 配置部署。

访问路径配置为 `https://fengyucode.zhangff.cn/student/`。学生页面、History 路由和静态文件均带此前缀；Agent 的浏览器接口为 `/student/api/`。教育平台接口继续走 `/prod-api`。内部换票与开发模拟接口在学生网关返回 404；SSE 禁用代理缓冲。

## 首次配置

1. 沿用现有 `DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS` secrets，以及 `DEPLOY_PORT`、`DEPLOY_NETWORK` variables。SSH 主机密钥必须预先核验。
2. 先发布现有 education 网关：`education-admin/deploy/nginx.admin.conf` 新增此前缀，代理到独立容器 `fengyucode-student-agent-web:8080`。它采用 Docker 动态解析，不要求 Agent 先启动。两个独立 workflow 若同时运行，Agent 的首次公开路径校验可能早于网关更新；网关更新后重跑 Agent 发布。
3. 服务器安装 Docker Compose、curl、flock、python3；已有外部网络默认 `crm-ai_default`。首次安装且数据库卷尚不存在时，脚本会在 `/project/fengyucode-student-agent/.pipeline/agent.env` 自动生成三个独立随机密钥，权限 `600`，不会输出到日志。已有文件绝不覆盖；已有数据库但配置丢失时拒绝生成新口令，须恢复备份。按 `release.env.example` 补齐真实服务配置并备份该文件。数据库口令使用单独生成的 32 字节以上十六进制值；服务集成密钥、JWT 签名密钥分别生成，至少 32 字符。密钥只保存在服务器，不能提交仓库。
4. 配置真实 Nacos 提示词、模型网关/模型名与成绩源服务。容器的就绪检查只证明数据库可用，不能代替真实聊天/报告验收。
5. 教育平台通过服务端桥接调用 Agent 的 `/internal/v1/tokens`，给当前已授权学生换取短期令牌。星球挂件使用 `/play/planet/agent-session`，独立学生端使用 `/biz/auth/agent-token`；服务端私网与集成密钥同步见 `../../education-admin/docs/AGENT_WIDGET_DEPLOYMENT.md`。

可选 variables：`AGENT_DEPLOY_BASE_DIR`（默认 `/project/fengyucode-student-agent`）、`AGENT_HTTP_PORT`（默认 `18089`）。Agent API、worker 与 PostgreSQL 没有宿主机公开端口；只有学生网关绑定 `127.0.0.1:18089`。服务器需要有权限拉取两个 GHCR 镜像；私有包须事先配置只读拉取凭据，不复用工作流临时令牌。

## 线上演示回复与正式模型

按当前线上演示要求，release Compose 的 `AGENT_DEMO_RESPONSES` 默认 `true`：仅使用本地确定性示例规则和演示回复提供者，不访问尚未配置的模型。该开关独立于 `AGENT_MOCK_MODE`，生产仍为 `production` 且 `AGENT_MOCK_MODE=false`，开发会话、固定模拟学生、模拟教育登录和 `/dev/` 路由不会因此开放。

演示挂件继续使用真实学生身份和独立短期 JWT，学生之间的聊天、报告权限保持隔离。没有可信本局成绩时不补造成绩或报告；示例回复必须明确标识为演示内容，不能据此声称已接通真实模型或完成真实测评。

后续补齐 Nacos 提示词、模型地址、模型名和密钥后，把 GitHub Actions variable `AGENT_DEMO_RESPONSES` 设为字符串 `false` 并重新发布即可切换正式提供者。也可在服务器执行发布脚本时传入同名环境变量，仅接受 `true` / `false`。脚本将取值写入本版本目录 `.env`，回退版本时沿用该版本的模式，不受当前终端变量影响。此非秘密开关由版本配置控制；放在 server-only `agent.env` 中的同名值不会覆盖版本配置。

## 发布与恢复

版本目录为 `releases/<不可变镜像标签>/deploy/`。脚本在部署锁内先拉镜像，启动数据库、执行 Alembic 迁移，再切换 API/worker/学生端；验证就绪、局域网与公开 HTTPS 版本标记，以及敏感接口 404 后，才更新 `current` 和 `previous` 链接。

声明 `AGENT_DEMO_RESPONSES` 的新版发布还会在容器内执行 `python -m src.demo.smoke`：演示模式下以隔离的部署验收身份调用真实令牌和聊天接口，正式模型模式跳过该演示请求；脚本只输出固定验收结果，不输出令牌。旧版本 `.env` 未声明此开关时，不执行新模块，保留旧版回退兼容。

切换失败或演示回复验收失败时恢复旧容器镜像；首次发布失败停止新应用容器。数据库卷保留。迁移失败不会更新版本链接。数据库结构不会自动降级，生产迁移必须向后兼容，并在变更前另做数据库备份。

需要人工回退时在服务器执行：

```sh
bash /project/fengyucode-student-agent/current/pipeline-deploy.sh rollback /project/fengyucode-student-agent
```

流水线的部署单元测试用替身 Docker/curl 验证迁移顺序、版本校验、失败恢复及主动回退；不连接生产服务器。本地模拟联调见 `agent-widget-sdk/README.md`。生产 Compose 强制关闭开发 mock；线上演示回复开关不会代替正式身份桥接。
