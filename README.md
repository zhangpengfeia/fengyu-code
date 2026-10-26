# Fengyu Code 项目仓库

本仓库按独立项目目录组织。星球与游戏项目已迁移到 [gamesoptionproject](gamesoptionproject/README.md)，后续项目可在根目录创建同级文件夹。

```text
code-ai/
├── .git/                         # 整个仓库共用的 Git 历史
├── .github/workflows/deploy.yml   # 星球源码检查（站点由教育流水线发布）
├── .gitignore
├── README.md
└── gamesoptionproject/
    ├── package.json
    ├── package-lock.json
    ├── src/
    ├── public/
    ├── scripts/
    └── deploy/
```

## 启动星球项目

在仓库根目录运行：

```sh
cd gamesoptionproject
npm ci
npm run dev
```

构建和检查也在 `gamesoptionproject/` 内运行：

```sh
npm run build
npm run format:check
python3 -m unittest discover -s deploy/tests -v
```

原有依赖和本地构建产物已随目录迁移。首次克隆或更新依赖后再运行 `npm ci`。

## 流水线与部署

GitHub Actions 保留在仓库根目录。星球源码工作流仅进行检查，教育工作流 `.github/workflows/education.yml` 负责平台与游戏的测试、镜像构建和线上发布，避免两个工作流互相覆盖网站。

统一域名的教育平台发布复用现有站点身份：

| 配置                   | 当前值                                  |
| ---------------------- | --------------------------------------- |
| GitHub 仓库            | `zhangpengfeia/fengyu-code`             |
| 访问域名               | `https://fengyucode.zhangff.cn`         |
| 服务器目录             | `/project/fengyucode`                   |
| 镜像                   | `ghcr.io/zhangpengfeia/fengyu-code/web` |
| Compose 项目与网络别名 | `fengyucode` / `fengyucode-web`         |
| 宿主机端口             | `127.0.0.1:18088`                       |

继续使用已有的 Actions Secrets、`production` Environment、Caddy 站点和回滚机制。详细操作见 [部署说明](gamesoptionproject/deploy/README.md)。

## 添加其他项目

学生游戏 Agent 后端位于 [student-game-agent](student-game-agent/README.md)，独立管理 Python 依赖、Nacos 游戏规则、问答流程和每局评价 Worker。学生网页及悬浮学伴组件位于 [student-web](student-web/README.md)。在后端运行 `uv run --locked python scripts/dev_mock.py --web-port 5176` 可启动完整 mock 联调环境，覆盖登录、聊天、模拟游戏结束、生成报告和报告追问。

在仓库根目录新增同级项目文件夹，各自维护依赖、锁文件、构建产物和部署文件。为新项目在 `.github/workflows/` 创建独立工作流，设置对应目录的路径过滤、工作目录、缓存锁文件和 Docker 上下文。

其他独立项目应使用各自部署身份。按当前同域名要求，教育平台有意接管 `/project/fengyucode` 和 `fengyucode-web`，后台在根路径，星球移至 `/play/planet/`；使用版本核对与自动回滚，其他项目不受影响。

## 智慧教育后台

学生响应式网页位于 [student-web](student-web/README.md)，使用 Vue 3 + TypeScript + Vite，按 `pages/`、`features/`、`shared/` 拆分页面、业务与基础组件。支持手机底部导航和电脑侧栏，包含课程、报告、排行榜、智能学伴与账号设置。在该目录运行 `npm ci` 后，使用 `npm run dev:preview` 查看明确标记的设计预览，或 `npm run dev` 连接现有后端。

新增独立项目 [education-admin](education-admin/README.md)，按优化需求说明书 V1.1 实现第一期：若依 Vue3、Spring Boot 3、MySQL SQL、课件游戏目录、学校账号与 RBAC 及游戏入口鉴权。前后端、SQL、游戏和部署目录分别独立维护。原星球项目已增加教育平台访问保护，迁移方式见 [教育平台部署说明](education-admin/docs/DEPLOYMENT.md)。
