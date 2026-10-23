# Fengyu Code 项目仓库

本仓库按独立项目目录组织。星球与游戏项目已迁移到 [gamesoptionproject](gamesoptionproject/README.md)，后续项目可在根目录创建同级文件夹。

```text
code-ai/
├── .git/                         # 整个仓库共用的 Git 历史
├── .github/workflows/deploy.yml   # 星球项目的检查、构建和部署
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

GitHub Actions 保留在仓库根目录，工作目录、npm 缓存锁文件和 Docker 构建上下文均指向 `gamesoptionproject/`。推送或 PR 修改该目录或对应工作流时触发检查；`main` 推送在检查通过后部署，也可手动运行工作流。

现有部署身份保持一致，目录迁移不需要重建服务器应用：

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

在仓库根目录新增同级项目文件夹，各自维护依赖、锁文件、构建产物和部署文件。为新项目在 `.github/workflows/` 创建独立工作流，设置对应目录的路径过滤、工作目录、缓存锁文件和 Docker 上下文。

独立部署的新项目应使用自己的服务器目录、Compose 项目名、网络别名、宿主机端口、域名、镜像名和并发分组，避免覆盖星球项目。不要给新项目复用 `/project/fengyucode` 或 `fengyucode-web` 服务身份。修改其他项目不会触发当前星球项目的流水线。
