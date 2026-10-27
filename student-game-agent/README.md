# 学生游戏 Agent

独立的学生游戏后端工程，参考 `ai-infrastructure-main/src/deal_pilot` 的功能分层重新整理，未复制教学课件、Notebook、沙箱、部署工具、网页搜索或实验 Webhook。现有教育平台和参考工程保持不变。

应用有两个入口：**学生自由提问 → 确认上下文 → 按需加载模板、游戏规则或已有报告 → 回答**；**游戏结束事件 → 获取权威本局数据 → 加载本局规则与评价模板 → 生成并保存报告**。两条链路共用 Prompt 解析服务，评价由独立 Worker 执行。独立网页及悬浮学伴组件在同级 `../student-web/`，后端不再包含前端占位目录。

## 工程目录

```text
student-game-agent/
├── src/
│   ├── app/             FastAPI 应用工厂、装配、依赖与路由
│   ├── config/          环境配置、游戏目录、Nacos Prompt 缓存
│   ├── agents/          自由问答、结构化评价与 LangGraph 状态
│   ├── services/        上下文、Prompt 解析、问答与评价任务
│   ├── backends/        数据库、ORM、聊天与评价 Repository
│   ├── integrations/    权威本局数据来源协议与 HTTP 适配器
│   ├── rag/             文本与图片检索扩展入口（当前关闭）
│   ├── mock/            仅本地联调：登录、数据源、规则与模型替身
│   ├── schemas/         请求、响应和可信业务事实
│   ├── security/        集成凭据、学生 JWT 与权限
│   ├── middlewares/     请求 ID、错误与响应策略
│   ├── utils/           模型网关与错误类型
│   └── workers/         独立评价 Worker
├── config/              games.yaml、prompts/、mock.json
├── migrations/          Alembic 数据库迁移
├── scripts/dev_mock.py  一键启动本地前后端联调
├── tests/               接口、业务、持久化与 mock 流程测试
├── deploy/              Dockerfile、Compose
├── .env.example
├── pyproject.toml
└── uv.lock
```

路由只负责 HTTP 入参、认证依赖与响应；Agent 不访问数据库；业务服务组织 Repository、Agent 和外部适配器。增加数据来源时放入 `integrations/`，增加检索实现时放入 `rag/`。不要把业务逻辑重新堆到 `app/main.py`。

## 一键 mock 联调（当前阶段）

首次安装依赖，分别在本工程执行 `uv sync --locked`，在 `../student-web` 执行 `npm ci`。然后从本工程启动：

```sh
uv run --locked python scripts/dev_mock.py --web-port 5176
```

- 学生端：<http://127.0.0.1:5176/login>；学校编号 `11000`，学号 `demo`，密码 `demo123`。
- API 文档：<http://127.0.0.1:8088/docs>。前端真实发送 HTTP/SSE 请求，经 Vite 代理进入本后端。
- 启动器自动迁移独立的 `data/mock/agent.db`，生成本地随机密钥并保存在权限为 0600 的 `data/mock/keys.json`；不会覆盖真实 `.env` 或真实业务数据库。目录已忽略提交。
- 后端加载 `config/mock.json` 中的学校、学生、课程、榜单、两条初始报告和本局指标。Mock 模型与本地规则替身不请求外部模型、Nacos 或游戏数据接口。
- 进入课程 → 选择关卡 → 点击“模拟结束本局并生成评价” → Worker 自动生成报告 → 查看报告 → 点击学伴追问。也可在学伴页面自由提问、询问指定游戏规则，或展开右下角悬浮学伴。
- 所有 mock 规则、成绩、徽章和回答均为联调样例，不作为正式玩法或真实学生结论。页面有 Mock 标识，报告 `generation_mode=MOCK`，接口带 `X-Data-Mode: mock`。
- Mock 登录换取 Agent 短期令牌发生在后端；浏览器不接触模型密钥、集成密钥或 JWT 签名密钥。

`Ctrl+C` 同时停止本次启动的 API 和前端，保留联调报告。端口占用时启动器报错，可用 `--api-port` / `--web-port` 指定空闲端口；不会停止已有服务。单独启动 API 使用 `make mock-api`，前端使用 `npm run dev:mock`。

运行中的端到端接口检查（默认检查 5176，可用 `STUDENT_MOCK_ORIGIN` 修改本地地址）：

```sh
cd ../student-web
npm run test:mock
```

该检查经过前端代理验证登录、令牌桥接、普通问答与幂等、游戏规则问答、游戏结束及后台评价、报告 SSE 追问、退出登录。它会增加一条明确标记的 mock 报告。

`AGENT_MOCK_MODE` 默认关闭，`production + mock` 拒绝启动，mock 路由在正常模式不注册。当前 mock 未实现改密、兑换等真实业务写入；这些入口不会伪造成功。`dev:preview` 是前端静态设计预览，与此真实 HTTP 联调模式不同。

切换真实服务时停止 mock 启动器，按下节配置 `.env`、Nacos、模型与权威数据接口，分别启动 API/Worker；前端使用 `npm run dev` 并配置真实教育平台令牌桥接。模型预留项为 `AGENT_LLM_BASE_URL`、`AGENT_LLM_API_KEY`、`AGENT_LLM_MODEL`，密钥仅放后端。

## 本地启动

使用 Python **3.13.11** 与 `uv`。以下命令从本工程目录执行：

```sh
uv python install 3.13.11
uv sync --locked
cp .env.example .env
```

编辑 `.env`，至少填写 `AGENT_SERVICE_API_KEY` 和 `AGENT_JWT_SECRET`。分别执行两次以下命令生成不同随机值，不共用密钥：

```sh
uv run --locked python -c 'import secrets; print(secrets.token_hex(32))'
```

SQLite 仅用于本机开发，数据位置为 `data/agent.db`。准备数据库并启动 API：

```sh
make migrate
make dev
```

已有数据库也要执行 `make migrate`，升级至 `0002`，以支持无游戏的普通会话及报告上下文；不要清库重建。数据库迁移不代替 Nacos 配置升级，开始业务联调前还需发布下文的新模板。

API 地址为 `http://127.0.0.1:8088`，交互文档为 `http://127.0.0.1:8088/docs`。另开终端，在同一工程目录启动评价 Worker：

```sh
make worker
```

仅启动 API 不会消费评价任务。模型、Nacos 或游戏结果接口尚未配置时，健康检查和接口文档仍可访问；需要这些依赖的业务请求会明确失败，不返回演示答案。`/health/ready` 检查数据库迁移状态，不代表已完成外部模型与 Nacos 连通性验证。

## Nacos 规则配置

使用 Nacos **3.2+** Prompt 客户端 API，示例 namespace 为 `student-game-agent`。`AGENT_NACOS_URL` 指向客户端 API 地址，例如 `http://127.0.0.1:8848`；不要填控制台的 8080 端口。命名空间和专用访问账号由运维创建，本工程不会自动更改已有 Nacos。

在 Nacos 发布以下 Prompt，初始版本均与本地配置对齐为 `1.0.0`：

| Prompt key | 内容来源 |
| --- | --- |
| `companion.base` | `config/prompts/companion.base.md`，通用表达与事实约束 |
| `companion.chat` | `config/prompts/companion.chat.md`，自由问答与报告追问任务 |
| `companion.evaluation` | `config/prompts/companion.evaluation.md` |
| `game.rules.eight-pieces` 等 | 经确认的真实玩法完整文本；绑定见 `config/games.yaml` |

`game-rules.md.example` 只是规则维护格式，不是可用的游戏规则。第一阶段使用完整 Prompt，不提供运行时任意变量执行；未填写的 `{{...}}`、空模板、错误版本和不存在的规则都会拒绝使用。缓存仅命中相同 key/version，不在请求失败后切换到其他版本。

`services/prompt_resolver.py` 将模板分成通用约束、场景任务和可选游戏规则三层。普通聊天只加载 `companion.base` 与 `companion.chat`；游戏问答再加载对应规则；报告追问按已授权报告的本局规则版本加载；自动评价使用通用约束、评价任务和本局规则，不携带聊天历史。

新配置使用 `AGENT_BASE_PROMPT_KEY/VERSION`、`AGENT_CHAT_PROMPT_KEY/VERSION` 与 `AGENT_EVALUATION_PROMPT_KEY/VERSION`。升级时先在 Nacos 发布 `companion.base` 和 `companion.chat` 的对应精确版本，再更新环境配置；旧 `AGENT_SYSTEM_PROMPT_*` 已被替换，不会自动回退到旧模板。现有 Nacos 中的旧资源可先保留，本工程不自动删除或发布资源。

已有模板版本保持不变。若要采用更新后的评价模板内容，应发布一个新版本并更新 `AGENT_EVALUATION_PROMPT_VERSION`，不要覆盖已发布的同版本内容；已经开始的任务仍复用原快照。

游戏问答默认使用游戏目录绑定的精确版本；已有报告追问和自动评价绑定事实中的 `game_rule_version`。每次成功回答和评价保存实际 Prompt 版本、SHA-256 摘要及内容快照，评价重试沿用已保存的快照。设置 `AGENT_PROMPT_PREWARM=true` 可在启动时预热公共模板；不会遍历全部游戏，预热失败记录日志，后续请求仍按需获取。

游戏资料链接：[二阶测评游戏开发第五册](https://www.kdocs.cn/l/csTOyvdB2EOJ)。本次已确认在线文档标题，但未能稳定读取表格单元格，**没有将未核实的在线玩法写入 Prompt**。目录中八个名称来自此前用户提供的 Word，仍需按在线表核对；游戏 code 可以调整，已产生记录后应保持稳定。

## 模型配置

填写 `AGENT_LLM_BASE_URL`、`AGENT_LLM_API_KEY`、`AGENT_LLM_MODEL`。可使用参考项目的 LiteLLM 网关，或兼容 Chat Completions 的模型服务；业务代码不固定供应商。内网模型如使用视觉能力，应另外验证接口和模型支持情况。

模型没有自动重试，由评价任务统一安排重试，避免多层重试放大调用次数。只向客户端返回最终文本，不返回第三方模型的推理扩展字段。自动评价不向模型发送学生、学校和来源本局标识。

## 鉴权和前端调用

第一阶段提供一个受信任宿主的集成入口，不另建学生账号密码库。**宿主后端必须先验证学生登录、学校状态和资源权限**，才能调用令牌接口；这里不接受浏览器自行声明的学生身份。

宿主后端请求 `POST /internal/v1/tokens`，请求头为 `X-Integration-Key: <AGENT_SERVICE_API_KEY>`，正文示例：

```json
{"student_id":"student-123","school_id":"school-123"}
```

返回 `access_token`，前端后续使用 `Authorization: Bearer <access_token>`。不要把集成密钥或 JWT 签名密钥交给前端。令牌默认有效 15 分钟；当前尚未接宿主撤销通知，停用身份的生效上界为剩余令牌有效期，正式统一登录需要接入宿主的撤销或实时校验能力。

普通聊天调用 `POST /api/v1/chat`，可以省略 `game_code`：

```json
{
  "request_id":"5b140774-1a32-4d43-ab95-228c8ec9fa4a",
  "question":"你好，我今天想聊聊学习。"
}
```

游戏问答可由页面提供 `game_code`，也可在问题中明确游戏名称：

```json
{
  "request_id":"5b140774-1a32-4d43-ab95-228c8ec9fa4a",
  "game_code":"eight-pieces",
  "question":"这个游戏怎么玩？"
}
```

上下文不足的问题（如首次只问“这个游戏怎么玩”）会先请求确认，不调用模型猜测；即使页面携带游戏标识，只说“你好”等普通聊天也不加载游戏规则。同一会话可通过明确名称切换讨论的游戏。

当前路由使用游戏名称/别名、页面或会话上下文及少量关键词，不增加模型分类调用。复杂表述建议由页面明确提供游戏或报告标识；同时提到多个游戏时会先请求选择。聊天历史按场景、游戏规则版本或本局报告过滤，避免把其他游戏或另一局的分析混入回答。

追问指定报告时传入本人报告任务的 UUID，例如：

```json
{
  "request_id":"5b140774-1a32-4d43-ab95-228c8ec9fa4b",
  "evaluation_id":"734de4c5-3376-498f-a606-c0e376775851",
  "question":"我这局为什么只拿到铜徽章？"
}
```

“我刚才为什么只有铜徽章”等问题也可按当前已确认上下文查询已有报告。后端校验学生和学校归属，再读取事实、报告和对应规则；没有报告或报告未就绪时直接说明，不伪造数据、不重新创建评价任务。

响应包含回答、会话 ID、Prompt/报告引用、`scene` 和 `needs_clarification`。场景为 `GENERAL_CHAT`、`GAME_QA` 或 `REPORT_QA`；`game_code`、`game_rule_version`、`evaluation_id` 和 `model` 在不适用时为 `null`。追问时传回 `conversation_id` 并生成新的 `request_id`。同一问题重试复用原 `request_id`，会返回已保存结果；同 ID 改写问题返回 409。同一会话不允许并发生成。学生与学校范围由 JWT 注入，不接受请求体覆盖。

`POST /api/v1/chat/stream` 提供 `status → answer → done` 的 SSE 事件；当前返回完整最终答案，**尚未实现逐 Token 推送**。事件流已开始后的业务错误通过 `error` 事件传递。前端使用带认证头的 `fetch`；断线后用原请求 ID 重试。学生网页已复用同一聊天组件实现响应式悬浮窗；面向第三方宿主的独立脚本 SDK、一次性嵌入票据和原生 App 桥接尚未实现。

## 每局结束自动评价

设置 `AGENT_ATTEMPT_API_URL`、`AGENT_ATTEMPT_API_KEY`。当前仅实现一种 HTTP 数据源，后续可按 `AttemptSource` 协议增加其他来源。

游戏后端先保存可信最终结果，再调用 `POST /internal/v1/game-events`，使用同一服务认证头：

```json
{"source_attempt_id":"attempt-001","source_revision":1}
```

事件只包含索引，不接受浏览器提交徽章或成绩。Worker 调用固定服务地址：

```text
GET {AGENT_ATTEMPT_API_URL}/attempts/{source_attempt_id}?revision={source_revision}
Authorization: Bearer {AGENT_ATTEMPT_API_KEY}
```

该接口应返回 `AttemptData` 对应的顶层 JSON 对象，而不是不确定的包装结构：

```json
{
  "source_attempt_id":"attempt-001",
  "source_revision":1,
  "student_id":"student-123",
  "school_id":"school-123",
  "game_code":"eight-pieces",
  "game_rule_version":"1.0.0",
  "volume_no":5,
  "mode":"ASSESSMENT",
  "difficulty":"NOVICE",
  "duration_ms":83000,
  "actual_steps":36,
  "optimal_steps":28,
  "badge":"BRONZE",
  "passed":true,
  "ended_at":"2026-10-26T02:00:00Z"
}
```

数字仅表示字段形态，不是实际评分阈值。来源接口负责验证成绩、身份归属、终态和规则版本；仅靠认证头不能使浏览器伪造成绩变为可信。若人机对战没有可比的最优步数，传 `null`。当前不采集操作轨迹或错误次数。

Worker 按数据版本去重、保存事实和规则快照，再生成评价；未就绪接口返回 202/404 时进入 `WAITING_DATA`，之后有限退避重试。模型失败时，仅在已有可信事实和有效规则后允许固定模板降级；数据或规则不完整时标记失败，不伪造报告。训练局仅保留跳过任务索引，不存学生归属、局内指标或评价。

升级前已开始的评价任务继续复用旧事实和提示词快照；旧版两项快照会显式归档当时的代码策略为 `legacy.code-boundary`，不拉取新 base 模板改变旧任务。报告追问优先使用报告保存的规则快照。两种场景均不会因 Nacos 发布新版本而改写已保存的报告。

同一局修订必须增加 `source_revision`，会生成独立历史记录，旧报告不覆盖；当前查询接口返回版本历史，未实现册次聚合、最新版本投影和排行榜。Worker 执行租约和数据库条件更新防止旧执行覆盖新执行；模型返回到落库之间崩溃可能重复调用模型，不承诺上游绝对只计费一次。

## RAG 扩展位置

`rag/schemas.py` 预留：文本查询、图片对象键、学生/学校范围、游戏、规则版本、top-k，以及文本/图片检索结果与来源引用。`rag/retriever.py` 定义 `Retriever`，默认 `DisabledRetriever` 返回空结果。

后续新增检索实现，在 `app/container.py` 装配，并按身份与游戏版本过滤。仅游戏问答进入检索扩展节点，普通聊天和报告追问不执行知识库检索；学生成绩始终从业务数据读取。文本片段可进入上下文、图片结果可返回引用。**向量库、文档入库、OCR、图片上传、图片向量检索和视觉模型调用尚未实现**；目前未直接读取图片内容，只能使用将来检索器提供的文字说明。`AGENT_RAG_ENABLED=true` 会明确拒绝启动，接入真实 Retriever 后再调整启用逻辑。

## 独立容器部署

Compose 只部署本应用 API、Worker、一次性迁移任务和独立 PostgreSQL，不会复制参考项目的整套基础设施。Nacos 和模型网关通过可配置地址接入；容器内不能用 `127.0.0.1` 访问宿主服务。Docker Desktop 可配置 `host.docker.internal`，Linux 使用受控网络中可达的服务地址。

先准备 `.env`，额外设置随机的 `POSTGRES_PASSWORD`（建议使用十六进制随机值，避免数据库 URL 特殊字符）。在本工程目录执行：

```sh
docker compose --env-file .env -f deploy/compose.yaml up --build -d
```

该命令会新建 `student-game-agent` Compose 项目、容器及 `agent_postgres` 命名卷，数据保存在 Docker 卷中，API 仅绑定 `127.0.0.1:8088`。迁移容器成功后才启动 API 与 Worker，不触碰教育平台数据库和参考项目数据卷。生产发布前由反向代理提供 HTTPS、请求限流和访问策略；不要直接公开内部集成入口。

停止并保留数据库：

```sh
docker compose --env-file .env -f deploy/compose.yaml stop
```

升级前备份数据库。回滚应用镜像保留数据卷；存在新迁移时先验证旧应用兼容性，不默认执行删除数据的降级。本次只生成部署文件，没有执行上述容器命令。

## 验证

从本工程目录执行：

```sh
make test
make check
```

自动化测试使用临时 SQLite、真实 Alembic 迁移、FastAPI ASGI、真实 LangGraph 和模型协议客户端；外部 Nacos/模型/测评接口使用 HTTP 协议替身，不需要也不会使用生产凭据。覆盖普通聊天与按需规则、报告追问、版本快照、越权拒绝、幂等、租约、SSE、训练跳过、数据延迟、来源修订和模型降级等行为。

开发使用 SQLite；Compose 使用 PostgreSQL。真实模型质量、Nacos 部署连通性、外部游戏接口、PostgreSQL 多进程并发、容器构建和真实教育平台前端接入需要在对应环境继续验收，不能用当前协议测试替代。

## 星球独立悬浮窗与发布

`uv run --locked python scripts/dev_mock.py --with-planet` 同时启动 Agent、学生应用与星球。开发专用 `/dev/mock/widget-session` 接收星球模拟事实，返回短期令牌和测评 ID；生产环境不注册此路由。详见 `../agent-widget-sdk/README.md`。

正式发布路径 `/student/`、GitHub Actions 配置、服务器凭据和回退步骤见 `deploy/README.md`。

## 线上已登录演示回复

`AGENT_DEMO_RESPONSES=true` 可在正式部署中启用确定性的中文“演示回复”，后续接入真实模型时设为 `false`。此模式只替换本地提示词和回答提供者，不依赖 Nacos 或模型网络；继续使用真实短期 JWT、数据库和学生/学校隔离。内部换票返回 `demo: true`，`/health/ready` 返回非敏感 `demo_responses` 状态，供界面与部署检查明确标记。

演示回复不代表正式游戏规则或个人能力分析。演示模式不接收新测评事件，测评 worker 暂停领取任务，不创建模拟成绩、徽章或报告。已有学生数据保留。`AGENT_MOCK_MODE` 仍必须关闭，生产环境禁止启用它；开发 bootstrap、固定示例学生和 `/dev/` 路由不会因演示回复开关而开放。

部署容器中可运行 `python -m src.demo.smoke` 验证真实内部换票与演示问答链路，只使用隔离的 `deployment-smoke` 合成身份；结果仅打印固定成功/失败提示，不打印密钥、令牌或回复正文。关闭演示模式后该检查跳过问答，不调用真实模型。
