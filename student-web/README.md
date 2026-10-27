# 枫语趣学 · 学生端

独立的 Vue 3 + TypeScript + Vite 学生前端，适配手机、平板和电脑浏览器。课程、报告、排行榜、AI 学伴与账号设置分别按功能与页面组织。

## 启动

需要 Node.js 20（建议 20.19+）或 22，npm。

```sh
cd student-web
npm ci
# 无需后端：明确展示示例数据及示例 AI 回复
npm run dev:preview
```

打开终端显示的本地地址，默认 `http://127.0.0.1:5175`。预览模式可以访问 `/login` 查看登录页，点击“进入设计预览”回到首页；不会提交密码、兑换资源、增加积分或调用真实模型。

连接真实后端：

```sh
cp .env.example .env.local
npm run dev
```

教育平台默认 `http://127.0.0.1:8081`，Agent 默认 `http://127.0.0.1:8088`，在 `.env.local` 中修改 `EDUCATION_API_TARGET`、`AGENT_API_TARGET`。真实模式必须登录，不会因网络或接口错误自动切换示例数据。真实游戏联调时，请给教育后端设置 `EDU_GAME_ORIGIN=http://127.0.0.1:5175`，否则游戏启动票据可能跳转到旧管理端的 5173 端口。全程使用同一个 hostname（例如都用 `127.0.0.1`），保证游戏 Cookie 正常发送。

```sh
npm test
npm run build
# 仅生成演示产物；正式业务使用上面的 build
npm run build:preview
```

## 前后端 mock 联调

本模式实际请求 FastAPI 后端，与 `dev:preview` 的静态设计预览分开。从同级后端一键启动：

```sh
cd ../student-game-agent
uv sync --locked
uv run --locked python scripts/dev_mock.py --web-port 5176
```

打开 <http://127.0.0.1:5176/login>，选择学生登录，学校编号 `11000`，学号 `demo`，密码 `demo123`。后端自动迁移独立的 mock 数据库并启动评价消费者，不需要真实模型、Nacos 或教育平台。

可验证：登录 → 自由提问；课程 → 关卡 → 模拟结束本局 → 生成报告 → 继续追问。右下角学伴按钮可展开同一套聊天面板。后端提供初始课程、榜单和两条报告，页面、回答与报告均显示 Mock 标识。改密、兑换不在 mock 支持范围，不返回虚假成功。

```sh
# 另一终端，服务保持运行
npm run test:mock
# 编译专用 mock 产物；正式部署仍用 npm run build
npm run build:mock
```

单独启动时：后端运行 `make mock-api`，前端运行 `npm run dev:mock -- --port 5176`。`MOCK_AGENT_API_TARGET` 指向本地 mock API，默认 8088；开发代理将 `/education-api` 映射到后端 `/dev/education`、`/agent-api` 映射到 Agent。仅 mock 模式默认使用后端 `/biz/auth/agent-token` 桥接，浏览器不包含任何服务密钥。真实模式继续使用 `VITE_AGENT_TOKEN_ENDPOINT`，不会在错误时自动降级为 mock。

配置与后端流程见 [后端说明](../student-game-agent/README.md)。

## 目录约定

```text
student-web/
├── public/images/               # 复用现有星球 WebP 素材
├── src/
│   ├── app/
│   │   ├── App.vue              # 应用入口、全局反馈
│   │   ├── router/              # 懒加载路由、登录/首次改密守卫
│   │   └── layouts/             # 桌面侧栏、顶部栏、手机底部导航
│   ├── pages/
│   │   ├── home/                # 首页，components/ 为首页私有组件
│   │   ├── courses/             # 课程目录、筛选
│   │   ├── course-detail/       # 课程详情、游戏关卡
│   │   ├── reports/             # 学习报告列表
│   │   ├── report-detail/       # 单局测评详情
│   │   ├── ranking/             # 班级排行、领奖台、搜索
│   │   ├── companion/           # 对话、导师、自定义学伴
│   │   ├── profile/             # 学生档案、荣誉、激活码兑换
│   │   ├── settings/            # 密码与退出登录
│   │   ├── login/               # 三种账号登录、验证码
│   │   └── not-found/           # 404
│   ├── features/
│   │   ├── auth/                # 登录类型、接口、Pinia 状态
│   │   ├── courses/             # 课程/游戏契约、映射、卡片、加载逻辑
│   │   ├── learning/            # 报告/排行契约、雷达图、奖章
│   │   ├── companion/           # SSE、聊天状态、历史、浏览器偏好
│   │   └── profile/             # 学生个人业务接口
│   ├── widget/                  # 学生站内悬浮窗入口，复用 features/companion
│   └── shared/
│       ├── api/                 # 请求、错误、教育/Agent 会话
│       ├── components/          # 图标、学伴头像、加载/错误/空态
│       ├── composables/         # 全局反馈等通用逻辑
│       ├── styles/              # 颜色、圆角、布局基础样式
│       └── utils/               # 资源路径、跳转地址校验
├── tests/                       # SSE 网络分片、跳转地址验证
└── deploy/                      # 同域学生端部署示例
```

依赖方向为 `app → pages → features → shared`。页面私有组件放在本页面 `components/`；一个业务内跨页面复用的组件放在对应 `features/*/components/`；不包含业务含义的通用组件放在 `shared/components/`。业务逻辑和请求不要写入全局布局。

## 已接入的真实接口

| 功能                     | 接口                                                                          |
| ------------------------ | ----------------------------------------------------------------------------- |
| 登录、本人资料、首次改密 | `/biz/auth/login`、`/biz/auth/me`、`/biz/auth/password`                       |
| 图形验证码、退出         | `/captchaImage`、`/logout`                                                    |
| 学校与本人班级           | `/biz/options`                                                                |
| 已授权课程/游戏          | `/biz/catalog/courseware`、`/biz/catalog/games`                               |
| 课程详情、游戏票据       | `/biz/catalog/courseware/{id}`、`/biz/games/{id}/launch`                      |
| 班级积分榜               | `/biz/learning/ranking`                                                       |
| 激活码兑换               | `/biz/learning/activation/redeem`                                             |
| AI 游戏目录、聊天、历史  | `/api/v1/games`、`/api/v1/chat/stream`、`/api/v1/conversations/{id}/messages` |
| 单局测评与导师建议       | `/api/v1/evaluations`、`/api/v1/evaluations/{id}`                             |

教育 API 使用若依响应体，`shared/api/client.ts` 统一检查业务 `code` 和 HTTP 状态，解包 `data`，列表保留 `rows/total`；Agent 保留其原始响应。主键按字符串处理。游戏通过后端返回的单次票据 URL 进入，不拼接公开游戏路径。

## Agent 登录桥接

Agent 不接受教育平台令牌，也没有学生账号密码登录接口。教育平台需增加一个学生可调用的换票接口：校验当前学生身份后，由后端携带集成凭据请求 Agent 的 `/internal/v1/tokens`，再返回 `{access_token, expires_in}`。Mock 联调已在 Agent 的开发专用路由实现此桥接；真实教育平台仍需实现对应接口。

将该教育平台接口路径填入 `VITE_AGENT_TOKEN_ENDPOINT`。默认留空；未接通时学伴和报告显示明确提示。此项目不猜测一个不存在的后端路径，也不会将服务集成密钥、模型密钥或 JWT 签名密钥放入前端。教育令牌仅保存在 sessionStorage，Agent 短期令牌仅保存在内存。

当前 SSE 发送处理状态和完整最终回答，前端实现停止等待、同请求 ID 重试、错误提示与分片解析，不模拟真实逐 Token 推送。

## 当前功能边界

- 能力百分比、册次趋势、学习天数与荣誉汇总暂无统一数据接口。真实模式显示单局事实和可获得的统计，雷达图与荣誉墙仅在明确的设计预览中展示示例。
- 校园榜当前后端拒绝学生访问，页面明确禁用校园榜；班级榜遵守现有角色权限。
- 学伴昵称、头像、语音偏好保存在当前浏览器，不跨设备同步。没有头像上传、语音合成或语音识别后端，不显示虚假的试听/上传成功。
- 站内悬浮窗位于 `src/widget/AgentWidget.vue`，与独立学伴页面复用 `features/companion/components/ChatPanel.vue`；面向星球的独立挂件位于 `../agent-widget-sdk`，模拟接入页面位于 `src/widget/frame/`。正式宿主鉴权仍需教育后端换票。
- 星球页面已复用仓库素材；真实游戏逻辑仍由现有 `/play/` 项目提供。

## 移动与桌面适配

767px 以下切换到底部导航、单列卡片；桌面显示侧栏与分栏内容；AI 对话在宽屏上附加提示区，窄屏保留主要聊天操作。处理刘海安全区、浏览器动态高度与减少动画偏好。图表按容器大小变化自动 resize，离开页面时释放实例。

## 部署

生产默认使用真实模式。与现有后台共域时，可设置 `VITE_BASE_PATH=/student/` 构建，把 `dist/` 内容放到服务器 `/usr/share/nginx/html/student/`。`VITE_EDUCATION_API_BASE=/prod-api` 可复用原教育平台 API 代理；Agent 另配 `/agent-api` 代理。

参见 `deploy/nginx.student.conf`（需要并入现有 server 块，不能直接覆盖现有管理端配置）。需保留 `/play/` 的逐请求鉴权代理。History 路由需回退到 `/student/index.html`；Agent SSE 代理需关闭缓冲并配置适当超时。

## 星球嵌入与发布流水线

星球模拟接入和启动步骤见 `../agent-widget-sdk/README.md`。独立 iframe 路由为 `/widget`，不加载站点导航。发布入口已配置为 `/student/`，构建与网关见 `deploy/Dockerfile`、`deploy/nginx.agent.conf`；服务器准备、身份桥接和发布/回退见 `../student-game-agent/deploy/README.md`。
