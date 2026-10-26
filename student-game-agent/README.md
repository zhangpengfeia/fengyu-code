# 学生游戏 Agent

独立的学生游戏后端工程，参考 `ai-infrastructure-main/src/deal_pilot` 的功能分层重新整理，未复制教学课件、Notebook、沙箱、部署工具、网页搜索或实验 Webhook。现有教育平台和参考工程保持不变。

当前主链路为 **学生提问 → 读取 Nacos 游戏规则 → 预留检索节点 → 调用大模型 → 保存并返回回答**。同时提供每局测评结束事件、权威数据适配器和独立评价 Worker。`frontend/` 和 `widget-sdk/` 为后续独立网页与悬浮窗预留，目前没有前端实现。

## 工程目录

```text
student-game-agent/
├── backend/
│   ├── src/student_game_agent/
│   │   ├── app/                         FastAPI 装配、依赖注入与路由
│   │   │   ├── main.py                  应用工厂
│   │   │   ├── container.py             基础设施和服务的统一装配
│   │   │   └── routes/                  health/auth/games/chat/evaluations
│   │   ├── config/                      环境配置、游戏目录和 Nacos
│   │   │   ├── settings.py
│   │   │   ├── game_catalog.py
│   │   │   └── nacos/prompts.py         认证、精确版本查询与缓存
│   │   ├── agents/                      Agent 状态与模型流程
│   │   │   ├── state.py
│   │   │   ├── question_agent.py        LangGraph 规则问答
│   │   │   └── evaluation_agent.py      结构化本局评价
│   │   ├── services/                    问答幂等、评价任务与重试
│   │   ├── backends/                    数据库、ORM、聊天与评价 Repository
│   │   ├── integrations/               权威测评数据来源契约与 HTTP 适配器
│   │   ├── rag/                         文本/图片检索契约，当前关闭
│   │   ├── schemas/                     请求、响应与业务事实数据模型
│   │   ├── security/                    服务凭据校验与短期学生 JWT
│   │   ├── middlewares/                 请求 ID、错误处理和响应策略
│   │   ├── utils/                       模型网关与通用错误类型
│   │   └── workers/                     独立评价 Worker 进程
│   ├── config/                          游戏绑定与可发布 Prompt 内容
│   ├── migrations/                      Alembic 数据库迁移
│   ├── tests/                           业务、接口、协议与持久化测试
│   ├── .env.example
│   ├── pyproject.toml
│   └── uv.lock
├── frontend/                            独立网页工程预留
├── widget-sdk/                          悬浮窗接入 SDK 预留
└── deploy/                              Dockerfile 与独立 Compose
```

路由只负责 HTTP 入参、认证依赖与响应；Agent 不访问数据库；业务服务组织 Repository、Agent 和外部适配器。增加数据来源时放入 `integrations/`，增加检索实现时放入 `rag/`。不要把业务逻辑重新堆到 `app/main.py`。

## 本地启动

使用 Python **3.13.11** 与 `uv`。以下命令从本工程目录执行：

```sh
cd backend
uv python install 3.13.11
uv sync --locked
cp .env.example .env
```

编辑 `.env`，至少填写 `AGENT_SERVICE_API_KEY` 和 `AGENT_JWT_SECRET`。分别执行两次以下命令生成不同随机值，不共用密钥：

```sh
uv run --locked python -c 'import secrets; print(secrets.token_hex(32))'
```

SQLite 仅用于本机开发，数据位置为 `backend/data/agent.db`。准备数据库并启动 API：

```sh
make migrate
make dev
```

API 地址为 `http://127.0.0.1:8088`，交互文档为 `http://127.0.0.1:8088/docs`。另开终端，在同一 `backend/` 目录启动评价 Worker：

```sh
make worker
```

仅启动 API 不会消费评价任务。模型、Nacos 或游戏结果接口尚未配置时，健康检查和接口文档仍可访问；需要这些依赖的业务请求会明确失败，不返回演示答案。`/health/ready` 检查数据库迁移状态，不代表已完成外部模型与 Nacos 连通性验证。

## Nacos 规则配置

使用 Nacos **3.2+** Prompt 客户端 API，示例 namespace 为 `student-game-agent`。`AGENT_NACOS_URL` 指向客户端 API 地址，例如 `http://127.0.0.1:8848`；不要填控制台的 8080 端口。命名空间和专用访问账号由运维创建，本工程不会自动更改已有 Nacos。

在 Nacos 发布以下 Prompt，初始版本均与本地配置对齐为 `1.0.0`：

| Prompt key | 内容来源 |
| --- | --- |
| `companion.system` | `backend/config/prompts/companion.system.md` |
| `companion.evaluation` | `backend/config/prompts/companion.evaluation.md` |
| `game.rules.eight-pieces` 等 | 经确认的真实玩法完整文本；绑定见 `backend/config/games.yaml` |

`game-rules.md.example` 只是规则维护格式，不是可用的游戏规则。第一阶段使用完整 Prompt，不提供运行时任意变量执行；未填写的 `{{...}}`、空模板、错误版本和不存在的规则都会拒绝使用。缓存仅命中相同 key/version，不在请求失败后切换到其他版本。

问答默认使用游戏目录绑定的精确版本，升级时在 Nacos 发布新版本并更新绑定。评价使用本局来源数据中的 `game_rule_version`，确保旧局按旧规则解释。每次成功回答和评价保存实际 Prompt 版本、SHA-256 摘要及内容快照。

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

调用 `POST /api/v1/chat`：

```json
{
  "request_id":"5b140774-1a32-4d43-ab95-228c8ec9fa4a",
  "game_code":"eight-pieces",
  "question":"这个游戏怎么玩？"
}
```

响应包含回答、会话 ID、Prompt 引用和模型标识。追问时传回 `conversation_id` 并生成新的 `request_id`。同一问题重试复用原 `request_id`，会返回已保存结果；同 ID 改写问题返回 409。同一会话不允许并发生成或切换游戏。学生与学校范围由 JWT 注入，不接受请求体覆盖。

`POST /api/v1/chat/stream` 提供 `status → answer → done` 的 SSE 事件；当前返回完整最终答案，**尚未实现逐 Token 推送**。事件流已开始后的业务错误通过 `error` 事件传递。前端使用带认证头的 `fetch`；断线后用原请求 ID 重试。悬浮窗 SDK、一次性嵌入票据、原生 App 桥接和界面自适应仍留待前端阶段实现。

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

同一局修订必须增加 `source_revision`，会生成独立历史记录，旧报告不覆盖；当前查询接口返回版本历史，未实现册次聚合、最新版本投影和排行榜。Worker 执行租约和数据库条件更新防止旧执行覆盖新执行；模型返回到落库之间崩溃可能重复调用模型，不承诺上游绝对只计费一次。

## RAG 扩展位置

`rag/schemas.py` 预留：文本查询、图片对象键、学生/学校范围、游戏、规则版本、top-k，以及文本/图片检索结果与来源引用。`rag/retriever.py` 定义 `Retriever`，默认 `DisabledRetriever` 返回空结果。

后续新增检索实现，在 `app/container.py` 装配，并按身份与游戏版本过滤。问答图已经留有 `retrieval` 节点，文本片段可进入上下文、图片结果可返回引用。**向量库、文档入库、OCR、图片上传、图片向量检索和视觉模型调用尚未实现**；目前未直接读取图片内容，只能使用将来检索器提供的文字说明。`AGENT_RAG_ENABLED=true` 会明确拒绝启动，接入真实 Retriever 后再调整启用逻辑。

## 独立容器部署

Compose 只部署本应用 API、Worker、一次性迁移任务和独立 PostgreSQL，不会复制参考项目的整套基础设施。Nacos 和模型网关通过可配置地址接入；容器内不能用 `127.0.0.1` 访问宿主服务。Docker Desktop 可配置 `host.docker.internal`，Linux 使用受控网络中可达的服务地址。

先准备 `backend/.env`，额外设置随机的 `POSTGRES_PASSWORD`（建议使用十六进制随机值，避免数据库 URL 特殊字符）。在本工程目录执行：

```sh
docker compose --env-file backend/.env -f deploy/compose.yaml up --build -d
```

该命令会新建 `student-game-agent` Compose 项目、容器及 `agent_postgres` 命名卷，数据保存在 Docker 卷中，API 仅绑定 `127.0.0.1:8088`。迁移容器成功后才启动 API 与 Worker，不触碰教育平台数据库和参考项目数据卷。生产发布前由反向代理提供 HTTPS、请求限流和访问策略；不要直接公开内部集成入口。

停止并保留数据库：

```sh
docker compose --env-file backend/.env -f deploy/compose.yaml stop
```

升级前备份数据库。回滚应用镜像保留数据卷；存在新迁移时先验证旧应用兼容性，不默认执行删除数据的降级。本次只生成部署文件，没有执行上述容器命令。

## 验证

从 `backend/` 执行：

```sh
make test
make check
```

自动化测试使用临时 SQLite、真实 Alembic 迁移、FastAPI ASGI、真实 LangGraph 和模型协议客户端；外部 Nacos/模型/测评接口使用 HTTP 协议替身，不需要也不会使用生产凭据。覆盖问答和版本快照、越权拒绝、幂等、租约、SSE、训练跳过、数据延迟、来源修订和模型降级等行为。

开发使用 SQLite；Compose 使用 PostgreSQL。真实模型质量、Nacos 部署连通性、外部游戏接口、PostgreSQL 多进程并发、容器构建和完整前端接入需要在对应环境继续验收，不能用当前协议测试替代。
