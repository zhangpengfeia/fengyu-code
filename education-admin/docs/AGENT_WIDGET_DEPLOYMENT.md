# 星球智能学伴的线上身份桥接

学生端和嵌入页面使用现有 `/student/` 入口。星球中的学伴 iframe 通过同源 `/play/planet/agent-session` 获取短期学生令牌；教育 API 必须从服务器游戏会话解析身份和权限，不能相信浏览器传来的学生编号、学校或成绩。

## 两个 API 的私网连接

正式 Compose 为教育 API 与 Agent API 增加专用外部网络 `fengyucode-agent-bridge`。两个部署脚本都会幂等创建该网络，并检查其 `Internal=true`、驱动为 `bridge`。Agent 在此网络的别名为 `fengyucode-student-agent-api`，教育侧 `EDU_AGENT_BASE_URL` 固定为 `http://fengyucode-student-agent-api:8088`。

仅两个 API 接入此网络：不接入 PostgreSQL、worker、迁移容器和网站网关，不增加宿主机端口，不将 Agent API 接入已有公共网关网络。原有后端网络保留，Agent 的真实模型和提示词服务仍需要按已有服务配置接入。

学生网站的 `/student/api/internal/` 与 `/student/api/dev/` 继续返回 404。Java 在私网直接调用 `/internal/v1/tokens`，不会通过公开学生网关绕过这两个限制。

## 服务端集成密钥同步

Agent 首次部署已生成 `/project/fengyucode-student-agent/.pipeline/agent.env`。教育部署脚本只读取其中 `AGENT_SERVICE_API_KEY`，将单个值写入自己的 `.pipeline/agent-bridge.env`，环境变量名为 `EDU_AGENT_SERVICE_API_KEY`。教育原有密码和令牌密钥不参与此次同步；Agent 的 JWT 签名密钥、数据库口令和模型密钥不会进入 Java 容器。

桥接文件权限为 `600`，采用同目录临时文件和原子替换，不输出密钥；拒绝符号链接、缺失/重复或过短的集成密钥。原始 Agent 配置不被修改。服务器 Agent 目录可通过执行教育部署脚本时的 `AGENT_DEPLOY_BASE_DIR` 环境变量覆盖。

如果首次安装时还没有 Agent 配置，教育网站仍可部署；桥接端点会返回明确的未配置提示。若已有桥接文件而 Agent 源文件暂时缺失，保留原桥接文件。Agent 部署后重新发布教育 API 即可同步。生产 release Compose 的第二个 `env_file` 默认 `/dev/null`，便于不依赖服务器私密文件的 CI 解析；实际部署脚本会把真实桥接文件路径写入版本目录 `.env`。

## 发布顺序与验收

1. 先发布 Agent/学生端，使新 iframe 与私网别名可用；确认 `/student/__release.txt` 和 `/student/api/health/ready`。
2. 再发布教育 API/星球，使 Java 取得匹配集成密钥并显示正式悬浮入口。两个流水线同时运行时，最终验收应等待二者都完成。
3. 匿名访问 `/play/planet/index.html` 和星球会话入口仍应拒绝；不允许通过 iframe 伪造学生身份。
4. 使用已获星球权限的学生登录并从菜单进入游戏，打开学伴，确认服务器身份、游戏切换和短期令牌过期刷新。
5. 公网 `/student/api/internal/v1/tokens` 与 `/student/api/dev/mock/widget-session` 必须保持 404；正式挂件不传入模拟成绩。

当前线上可按演示要求开启 `AGENT_DEMO_RESPONSES=true`，提供明确标注的示例回复，身份仍来自真实学生会话。该模式不开放开发模拟路由，不使用固定模拟学生，也不补造本局成绩或报告。模型/提示词服务接通后，把发布变量设为 `false` 并重新发布。

数据库就绪检查不代表真实模型/提示词服务已接通。正式模型模式下缺少配置应显示中文配置提示，不应降级调用开发模拟路由。
