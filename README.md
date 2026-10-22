# AIM 趣智 · 数创星球

Vue 3 + TypeScript + Vite + Less 的交互式星球目录，使用提供的 PDF 切图制作的透明素材。原 PNG 保留，页面使用缩小并压缩的 WebP。

## 启动

```sh
npm install
npm run dev
```

打开终端输出的本地地址。生产构建：

```sh
npm run build
npm run preview
```

## 交互

- 背景地球固定，12 册星球在同一页的三条倾斜椭圆轨道上缓慢公转，每条轨道四颗星球；远处缩小，移动到前景中央时放大。
- 点击星球或底部册数按钮：沿所属轨道平滑移动到前景中央，再按位置放大；聚焦完成后自动继续公转。
- 星球默认自动公转，无需播放或暂停操作。
- 左右方向键在全部 12 册间切换；右下角“切换类型展示”在轨道与 6 列 × 2 行网格之间切换，保留选中册数。
- “探索第 X 册”进入对应游戏目录，背景星球与进入的册数一致，每册 8 个游戏入口。支持卡片选择、训练／测评切换和全部游戏网格视图；返回后保留原星球选择。
- 手机始终使用横向场景；竖屏打开时画面旋转 90°，横持时正常显示，保留选中状态并适配刘海安全区。
- 支持键盘操作和系统减少动态效果偏好。

## 文件

- `src/App.vue`：仅作为应用入口，挂载 `PlanetExperience`。
- `src/components/PlanetExperience.vue`：星球目录与册数游戏目录的导航，支持浏览器返回和 `#book/3` 直达链接。
- `src/components/LandscapeStage.vue`：共用 16:9 舞台、移动端强制横屏和安全区适配。
- `src/components/PlanetExplorer.vue`：组合场景组件，连接状态与事件，控制横竖屏场景比例。
- `src/components/SceneBackdrop.vue`：固定地球、背景、星光和轨道装饰。
- `src/components/ExplorerHeader.vue`：用户信息、标题和 12 册目录说明。
- `src/components/PlanetOrbit.vue`：12 颗星球的轨道布局、纵深和中央放大。
- `src/components/PlanetGrid.vue`：全部 12 册的 6 列 × 2 行网格展示。
- `src/components/OrbitTracks.vue`：与星球位置共用几何计算的前后两层轨道线。
- `src/config/scene.ts`：1200 × 675 设计坐标、地球尺寸、三条轨道及各轨道星球尺寸、十二颗星球的轨道分配和路径生成。
- `src/components/PlanetCard.vue`：单颗星球展示与选择。
- `src/components/ExplorerControls.vue`：探索入口、12 册导航和展示类型切换。
- `src/components/books/Book01.vue` 至 `Book12.vue`：12 册独立组件入口。
- `src/components/catalog/`：共用游戏目录、背景、游戏卡片与模式切换组件。
- `src/config/books.ts`：12 册主题与各自的 8 个游戏配置。
- `src/composables/usePlanetOrbit.ts`：轨道动画、12 册选中状态和生命周期清理。
- `src/assets.ts`：素材映射与 `Planet` 类型。
- `src/styles/global.less`：全局基础样式和减少动画偏好。
- `src/styles/variables.less`：Less 变量、mixin 和横竖屏断点；组件使用 scoped Less 管理各自样式。
- `public/assets/manifest.json`：PNG 与原 PDF 的对应关系。
- `scripts/convert-assets.swift`：macOS CoreGraphics 转换脚本，保留透明背景，不修改原始 PDF。

重新转换素材（macOS）：

```sh
swift -module-cache-path /private/tmp/aim-swift-cache scripts/convert-assets.swift '/你的路径/UI10.11' "$PWD/public/assets"
```

`scripts/trim-assets.py` 可用 Pillow 去除星球 PNG 的透明页边距。

已转换星球和第三册目录中共 39 份 PDF，PNG 已随工程提供，启动项目不需要原素材目录。

## 图片加载优化

页面使用的 16 张素材由约 5.32 MB PNG 压缩为约 712 KB WebP，保留透明背景。星球最长边 768px，地球 1440px，标题 1600px，头像 640px；页面仍按原宽高比显示。

- 首屏等待全部十二颗星球和四张公共图片，下载并解码完成后展示场景、启动公转；等待时显示进度，失败可重试。
- 切换轨道／网格视图时复用已解码素材，不重新加载；下载和解码并发受限。
- WebP 由 Vite 生成内容哈希文件名，生产 Nginx 缓存一年；更新素材后 URL 自动变化。PNG 原素材不使用永久缓存。
- 页面中的图片使用异步解码提示。图片优化减少下载量和解码尺寸，实际耗时仍受手机网络和设备性能影响。

优化产物已随仓库提供，CI 不需要 Pillow。更新原 PNG 后，可在安装 Pillow 的 Python 环境重新生成：

```sh
python3 scripts/optimize-assets.py
```

素材输出为 `src/assets/optimized/`。加载流程见 `src/composables/useSceneAssets.ts`，加载界面见 `src/components/SceneLoading.vue`。

## 12 册游戏目录

每册有独立入口组件 `src/components/books/Book01.vue` 至 `Book12.vue`，通过 `books/index.ts` 注册。共用 `BookGameCatalog.vue` 展示 8 个可选游戏、前后切换、训练／测评模式和网格视图。首页保留在内存中，进入游戏目录时停止首页动画，返回后继续。

第三册和第五册使用提供的森林／水果 PDF 切图，还原无文字的大星球、环境装饰和绿色／金色游戏卡片。其余十册先复用首页对应星球作为背景，并显示 8 个游戏占位卡片，后续可单独修改对应册组件与数据。

`src/config/books.ts` 中每册的 `games` 是独立数组，每个游戏有唯一 `id`、`title`、`thumbnail` 和 `entries`。当前素材里的预览图重复显示“铺马路”，暂共用对应册的预览切图；除参考图中的“抢石子”外，其余名称先用编号占位。当前没有实际游戏实现或 URL，点击开始会提示内容正在准备中。接入真实游戏时分别配置两种模式，例如：

```ts
// 在 books.ts 的 books 初始化之后配置（URL 仅为格式示例）。
books[2]!.games[0]!.entries = {
  training: '/games/book-03/game-01?mode=training',
  assessment: '/games/book-03/game-01?mode=assessment',
}
```

新增切图置于 `src/assets/catalog/` 后更新对应配置。已生成 WebP 会随工程打包，部署不依赖原 PDF。原素材映射见 `public/assets/catalog-manifest.json`。重新转换需要 Pillow 和 pypdfium2：

```sh
python3 scripts/convert-catalog-assets.py '/你的路径/UI10.11'
```

## 动画参数

`src/config/scene.ts` 统一地球大小、三条轨道的星球尺寸、中心、半径和倾角；`PLANET_SLOTS` 将十二颗星球分到三条轨道，每条四颗。轨道 SVG 与 `PlanetOrbit.vue` 共用 `orbitPoint()` 几何计算，`planetPosition()` 选择所属轨道，`planetScale()` 根据前景中央位置控制放大倍率。`usePlanetOrbit.ts` 中 `ORBIT_SPEED` 控制公转速度；网格模式暂停轨道运动。背景地球独立于动画层，不参与旋转或缩放。三条轨道按参考图采用宽幅构图，允许轨道和星球经过场景边界。

原切图已包含册数文字，因此采用图案保持正向的轨道公转，避免旋转整张图片导致文字倒置。若需球体纹理自转，可替换为不含文字的球体素材并单独叠加册数标签。

## 横竖屏适配

场景始终使用 16:9 横向布局，并同时受可用宽、高约束。650px 以下的手机竖屏视口中，`landscape-viewport` 交换可用宽高并旋转 90°，即使微信等内置浏览器锁定竖屏也能呈现横向场景；物理视口转横后自动恢复正常方向。触控由浏览器按 CSS 变换后的实际位置处理，切换方向时不重建组件。

手机紧凑控件同时应用于旋转的竖屏和低高度横屏。星球目录和游戏目录共用同一个横屏舞台，进入与返回时方向一致。使用动态视口高度 `100dvh`（回退 `100svh`）及 `env(safe-area-inset-*)` 适配浏览器工具栏和刘海安全区。此方案旋转网页内容，不依赖浏览器的屏幕方向锁定 API。

## 代码格式化

前端源码、Less、HTML、JSON 和 Markdown 使用 Prettier：

```sh
npm run format
npm run format:check
```

统一使用 2 空格缩进、单引号、无分号和 100 字符行宽（JSON 使用标准双引号）。依赖目录、构建产物和图片不参与格式化。

素材工具脚本分别使用 Ruff 和 macOS 自带的 swift-format：

```sh
uvx ruff format --line-length 100 scripts/trim-assets.py
xcrun swift-format format --in-place scripts/convert-assets.swift
```

## 自动部署到腾讯云

已配置 GitHub Actions：推送 `main` 后检查、构建 Docker 镜像并部署至 `152.136.228.231`，目标地址为 `https://fengyucode.zhangff.cn`。沿用现有「基金agent」项目的 Caddy 网关和 `DEPLOY_*` Secrets 命名。

首次需要配置子域名 DNS、新仓库 Secrets，并将 Caddy 站点片段加入现有网关项目。完整配置、触发方式及回滚步骤见 [部署说明](deploy/README.md)。
