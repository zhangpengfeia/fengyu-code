# 星球独立学伴挂件

浏览器 SDK 不依赖 Vue，通过 iframe 加载 `student-web` 的 `/widget` 页面。星球只维护当前游戏与模拟成绩，聊天、报告、短期令牌由 Agent 应用处理。

## 本地联调

分别在 `agent-widget-sdk`、`student-web`、`gamesoptionproject` 执行 `npm ci`，在 `student-game-agent` 执行 `uv sync --locked`。然后从仓库根目录执行：

```sh
cd student-game-agent
uv run --locked python scripts/dev_mock.py --with-planet --api-port 8091 --web-port 5180 --planet-port 5181
```

打开 `http://127.0.0.1:5181/`，点击右下角“智能学伴”。窗口收到当前册次、游戏、学生和成绩；“发送新一局模拟成绩”会变更局次与成绩并创建新报告。选择册次、游戏和训练/测评模式会同步更新上下文。接收数据摘要可展开查看服务端确认的用时、步数与徽章。“本局报告”问答使用本局评价 ID；训练模式不创建报告。

启动器仅监听本机，使用隔离的模拟数据库与自动生成的开发凭据。端口冲突会直接退出；可更换上述端口，不会关闭其他项目进程。退出启动器会同时停止它启动的三个服务。

## 接入结构

- `src/index.ts`：`createAgentWidget({container, url, context, onMockRound?, onAccepted?})`，返回 `open/close/updateContext/destroy`。`npm run build` 生成无框架的 `dist/agent-widget.js`，全局名为 `FengyuAgentWidget`。
- `src/protocol.ts`：上下文结构和校验。用 `crypto.randomUUID()` 生成 `requestId`；同一局次的成绩不可修改，新成绩使用新 ID。
- `gamesoptionproject/src/integrations/agent-widget/`：星球状态适配、模拟成绩工厂、窗口挂载与横竖屏适配。
- `student-web/src/widget/frame/`：独立嵌入页面、开发会话建立与聊天组件复用。
- Agent `/dev/mock/widget-session`：接收宿主事实，签发当前模拟学生令牌；测评进入实际报告工作流，重复请求幂等。

消息双方校验 `origin`、`source`、随机 `channelId` 与协议版本；iframe URL 不携带令牌。学伴端通过 `VITE_WIDGET_PARENT_ORIGINS` 明确列出允许的宿主源，逗号分隔。生产构建不包含模拟挂件入口，正常星球票据鉴权保留。这里的模拟 bootstrap 不能用于正式接入；正式版须由教育后端校验学生身份后换取短期 Agent 令牌，不能信任浏览器传入的学生 ID 或成绩。

## 正式线上挂件

生产星球右下角显示“智能学伴”，加载同域 `/student/widget`。宿主发送 `mode: live` 的册次、目录名称与游戏模式，不发送学生身份或成绩。iframe 通过 `/play/planet/agent-session` 的游戏 Cookie 获取服务器确认的学生和短期令牌；未登录、无资源权限、会话过期、非学生账号均按现有权限拒绝。令牌只驻留内存，过期后仍使用同一个游戏会话刷新。

真实目录可在 `BookGame.agentCode` 中绑定经过核对的 Agent 游戏代码。尚未绑定的“抢石子”等目录不会套用本地八子归位等模拟槽位映射，先使用通用学习问答。正式模式不接收浏览器模拟成绩，不创建虚构测评，也不开放模拟按钮。

Java 与 Agent 仅通过专用私有 Docker 网络连接，集成密钥仅配置于服务器。正式模型和 Nacos 提示词仍需配置；缺失时聊天明确提示服务未就绪。部署细节见 `education-admin/docs/AGENT_WIDGET_DEPLOYMENT.md`。
