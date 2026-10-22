# GitHub Actions → 腾讯云

目标仓库：`zhangpengfeia/fengyu-code`  
服务器：`152.136.228.231`，SSH 用户 `root`  
访问地址：`https://fengyucode.zhangff.cn`

部署沿用「基金agent」项目的 Docker、GHCR、SSH、生产环境和 Caddy 网关模式。新应用使用独立的 `fengyucode` Compose 项目，默认目录 `/project/fengyucode`，加入已有的 `crm-ai_default` Docker 网络。

## 1. 添加子域名 DNS

在 `zhangff.cn` 的 DNS 控制台添加：

| 类型 | 主机记录   | 记录值          |
| ---- | ---------- | --------------- |
| A    | fengyucode | 152.136.228.231 |

验证：

```sh
dig +short fengyucode.zhangff.cn A
```

应返回 `152.136.228.231`。服务器已有 Caddy 占用公网 80/443，腾讯云安全组和主机防火墙需允许这两个端口，供 Caddy 申请和续期该子域名的证书。

## 2. 接入现有 Caddy（首次一次）

现有网关由「基金agent」项目的 `deploy/Caddyfile` 管理，且每次发布都会从该项目更新。将本项目 `deploy/Caddyfile.snippet` 的完整内容追加到**该项目的源文件** `deploy/Caddyfile`，作为独立站点块：

```caddyfile
fengyucode.zhangff.cn {
    encode zstd gzip
    reverse_proxy fengyucode-web:8080
}
```

已根据你提供的原文件生成 `deploy/crm-ai-caddy.patch`。也可以在本机应用补丁，先检查再修改原项目：

```sh
git -C '/Users/a123/D盘/学习/project/基金agent' apply --check '/Users/a123/D盘/学习/project/code-ai/deploy/crm-ai-caddy.patch'
git -C '/Users/a123/D盘/学习/project/基金agent' apply '/Users/a123/D盘/学习/project/code-ai/deploy/crm-ai-caddy.patch'
```

然后按该项目原来的流水线发布一次，让现有 Caddy 加载新入口。只编辑服务器某个旧版本目录里的 Caddyfile，下一次原项目部署时可能被替换。

新项目的 Compose 服务名和网络别名均为 `fengyucode-web`，避免与共享网络中原项目的 `web` 服务重名。参考项目的 Compose 名称为 `crm-ai`，默认网络因此配置为 `crm-ai_default`。首次在服务器上检查实际网络：

```sh
docker network ls
docker network inspect crm-ai_default
```

若现有 gateway 使用其他网络，在新仓库的 Actions Variables 中设置 `DEPLOY_NETWORK` 为它所在的网络。

这里的 `deploy/nginx.conf` 仅供新应用容器内部使用，监听 8080；宿主机只绑定 `127.0.0.1:18088`，用于健康检查。

## 3. 在新仓库配置 Actions Secrets

与参考项目沿用相同名称；Secrets 需要在**新仓库**或新仓库的 `production` Environment 中配置，其他仓库的 Secret 不会自动共享。

打开：`https://github.com/zhangpengfeia/fengyu-code/settings/secrets/actions`

| Secret               | 值                                                                |
| -------------------- | ----------------------------------------------------------------- |
| `DEPLOY_HOST`        | `152.136.228.231`                                                 |
| `DEPLOY_USER`        | `root`                                                            |
| `DEPLOY_SSH_KEY`     | 能登录服务器的 SSH 私钥完整内容；可沿用原流水线使用的密钥         |
| `DEPLOY_KNOWN_HOSTS` | 经核对的服务器 known_hosts 完整记录；端口一致时可沿用原流水线的值 |

不要将私钥提交到 Git，也不要把私钥或服务器密码发到聊天中。

如需重新获取主机公钥，在**腾讯云控制台登录服务器后**运行：

```sh
awk '{print "152.136.228.231 " $1 " " $2}' /etc/ssh/ssh_host_ed25519_key.pub
```

将输出的公钥记录写入 `DEPLOY_KNOWN_HOSTS`。自定义 SSH 端口时记录的主机部分应为 `[152.136.228.231]:端口`。

可选 Actions Variables：

| Variable           | 默认值                |
| ------------------ | --------------------- |
| `DEPLOY_PORT`      | `22`                  |
| `DEPLOY_BASE_DIR`  | `/project/fengyucode` |
| `DEPLOY_NETWORK`   | `crm-ai_default`      |
| `DEPLOY_HTTP_PORT` | `18088`               |

服务器需要已有 Docker Compose v2、`flock`、`curl`。如 GHCR 新镜像包为私有包，服务器须具备拉取权限：使用对该包拥有 `read:packages` 权限的凭据执行 `docker login ghcr.io`。已有项目的登录凭据只有在同样能读取新包时才可直接复用。

## 4. 推送 main 或手动部署

先完成以上 DNS、Caddy 和 Secrets 配置，再提交并推送本项目到 `main`。流水线会：

1. 安装锁定依赖，检查格式、TypeScript 和前端构建，执行部署恢复测试。
2. 构建 Linux AMD64 镜像，验证 Nginx 及首页产物，发布到 `ghcr.io/zhangpengfeia/fengyu-code/web`。
3. SSH 上传版本配置，先拉取镜像，再更新独立的 `fengyucode` web 服务。
4. 验证容器健康、镜像版本标记，以及 Caddy 的真实 HTTPS 路由和证书。
5. 只有检查成功才记录 `current` 与 `previous`；失败时恢复上一版，首次失败则移除新容器。

PR 只运行验证，不访问部署凭据，也不发布镜像。手动触发：仓库 **Actions → Build and deploy Fengyu Code → Run workflow**，选择 `main`。

构建任务通过 job outputs 将实际发布的镜像标签传给部署任务。只重新运行失败的部署任务时，继续使用原构建标签，避免 `GITHUB_RUN_ATTEMPT` 增加后拉取不存在的镜像。修改流水线后需推送新提交触发新运行；重新运行旧记录仍使用旧版流水线。

当前尚未替你推送 Git 提交、配置 GitHub Secrets、修改 DNS 或修改原项目的 Caddyfile；这些凭据与入口准备完成后，推送 `main` 即会触发部署。

## 5. 排查与手动回滚

### 依赖安装停留在 npm ci

仓库 `.npmrc` 固定使用官方 npm registry，并将单次网络请求超时设为 30 秒、最多重试 2 次。Actions 的安装步骤上限为 6 分钟，并输出 Node/npm 版本、registry、下载请求和安装脚本日志；Docker 构建使用相同配置。安装命令优先复用 npm 缓存，并跳过本次安装的 audit 请求与 fund 提示。

如果后续仍失败，查看最后的下载 URL、`ETIMEDOUT`、`ECONNRESET`、`EAI_AGAIN` 等网络错误，或最后一个安装脚本名称。`The operation was canceled` 仅说明任务被取消，还需结合运行页面的取消原因判断。以上调整用于限制等待并暴露卡点，不能据此认定远端取消问题已解决。

### 容器与版本回滚

服务器查看当前容器：

```sh
cd /project/fengyucode/current
docker compose --project-name fengyucode ps
docker compose --project-name fengyucode logs --tail=100 fengyucode-web
curl -fsS http://127.0.0.1:18088/__release.txt
curl -fsS https://fengyucode.zhangff.cn/__release.txt
```

两个版本标记应一致。若容器正常但 HTTPS 验证失败，检查子域名解析、原 Caddyfile 是否已发布新站点，以及网关与 web 是否在同一 Docker 网络。

部署过至少两个成功版本后，可回滚到前一版：

```sh
bash /project/fengyucode/.pipeline/rollback.sh rollback /project/fengyucode
```

回滚同样执行健康检查；失败时会恢复操作前的版本。历史版本目录和镜像暂不自动清理。

## 参考

- [GitHub Actions Secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
- [Caddy 自动 HTTPS](https://caddyserver.com/docs/automatic-https)
- [腾讯云 DNS 解析](https://cloud.tencent.com/document/product/302/3446)
