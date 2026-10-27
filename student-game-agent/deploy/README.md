# 学生端与 Agent 发布

流水线为 `.github/workflows/student-agent.yml`：拉取请求执行测试与构建；推送 `main` 或手动运行时，发布两个不可变版本镜像并使用已有 SSH 配置部署。

访问路径配置为 `https://fengyucode.zhangff.cn/student/`。学生页面、History 路由和静态文件均带此前缀；Agent 的浏览器接口为 `/student/api/`。教育平台接口继续走 `/prod-api`。内部换票与开发模拟接口在学生网关返回 404；SSE 禁用代理缓冲。

## 首次配置

1. 沿用现有 `DEPLOY_HOST`、`DEPLOY_USER`、`DEPLOY_SSH_KEY`、`DEPLOY_KNOWN_HOSTS` secrets，以及 `DEPLOY_PORT`、`DEPLOY_NETWORK` variables。SSH 主机密钥必须预先核验。
2. 先发布现有 education 网关：`education-admin/deploy/nginx.admin.conf` 新增此前缀，代理到独立容器 `fengyucode-student-agent-web:8080`。它采用 Docker 动态解析，不要求 Agent 先启动。两个独立 workflow 若同时运行，Agent 的首次公开路径校验可能早于网关更新；网关更新后重跑 Agent 发布。
3. 服务器安装 Docker Compose、curl、flock、python3；已有外部网络默认 `crm-ai_default`。首次安装且数据库卷尚不存在时，脚本会在 `/project/fengyucode-student-agent/.pipeline/agent.env` 自动生成三个独立随机密钥，权限 `600`，不会输出到日志。已有文件绝不覆盖；已有数据库但配置丢失时拒绝生成新口令，须恢复备份。按 `release.env.example` 补齐真实服务配置并备份该文件。数据库口令使用单独生成的 32 字节以上十六进制值；服务集成密钥、JWT 签名密钥分别生成，至少 32 字符。密钥只保存在服务器，不能提交仓库。
4. 配置真实 Nacos 提示词、模型网关/模型名与成绩源服务。容器的就绪检查只证明数据库可用，不能代替真实聊天/报告验收。
5. 教育平台尚需实现学生身份换票桥接：后端调用 Agent 的 `/internal/v1/tokens`，对浏览器返回短期令牌。将这个教育 API 路径配置为 GitHub variable `AGENT_TOKEN_ENDPOINT` 后重建学生镜像。留空时学生端可加载，学伴/报告会提示尚未配置；不会伪造登录成功。

可选 variables：`AGENT_DEPLOY_BASE_DIR`（默认 `/project/fengyucode-student-agent`）、`AGENT_HTTP_PORT`（默认 `18089`）。Agent API、worker 与 PostgreSQL 没有宿主机公开端口；只有学生网关绑定 `127.0.0.1:18089`。服务器需要有权限拉取两个 GHCR 镜像；私有包须事先配置只读拉取凭据，不复用工作流临时令牌。

## 发布与恢复

版本目录为 `releases/<不可变镜像标签>/deploy/`。脚本在部署锁内先拉镜像，启动数据库、执行 Alembic 迁移，再切换 API/worker/学生端；验证就绪、局域网与公开 HTTPS 版本标记，以及敏感接口 404 后，才更新 `current` 和 `previous` 链接。

切换失败恢复旧容器镜像；首次发布失败停止新应用容器。数据库卷保留。迁移失败不会更新版本链接。数据库结构不会自动降级，生产迁移必须向后兼容，并在变更前另做数据库备份。

需要人工回退时在服务器执行：

```sh
bash /project/fengyucode-student-agent/current/pipeline-deploy.sh rollback /project/fengyucode-student-agent
```

流水线的部署单元测试用替身 Docker/curl 验证迁移顺序、版本校验、失败恢复及主动回退；不连接生产服务器。本地模拟联调见 `agent-widget-sdk/README.md`。生产 Compose 强制关闭 mock，不能用开发会话替代正式身份桥接。
