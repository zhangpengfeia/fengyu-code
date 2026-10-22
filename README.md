# AIM 趣智 · 数创星球

Vue 3 + TypeScript + Vite + Less 的交互式星球目录，使用提供的 PDF 切图转换而来的透明 PNG。

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

- 背景地球固定，册数星球沿椭圆轨道缓慢公转，根据轨道深度改变大小和前后遮挡。
- 点击星球或底部册数按钮：平滑移动到前景并放大；轨道停留在选中位置，方便继续操作。
- 点击右上角暂停/播放按钮：暂停动画，或重新开始公转。
- 左右方向键切换前后册；右下角按钮切换 1–6 册、7–12 册。
- “探索第 X 册”打开对应册数的演示弹窗，后续可接入课程目录。当前未接入实际课程。
- 手机始终使用横向场景；竖屏打开时画面旋转 90°，横持时正常显示，保留选中状态并适配刘海安全区。
- 支持键盘操作和系统减少动态效果偏好。

## 文件

- `src/App.vue`：仅作为应用入口，挂载 `PlanetExplorer`。
- `src/components/PlanetExplorer.vue`：组合场景组件，连接状态与事件，控制横竖屏场景比例。
- `src/components/SceneBackdrop.vue`：固定地球、背景、星光和轨道装饰。
- `src/components/ExplorerHeader.vue`：用户信息、标题和播放/暂停按钮。
- `src/components/PlanetOrbit.vue`：轨道布局、纵深缩放和遮挡。
- `src/components/OrbitTracks.vue`：与星球位置共用几何计算的前后两层轨道线。
- `src/config/scene.ts`：1200 × 675 设计坐标、地球与星球尺寸、倾斜椭圆和路径生成。
- `src/components/PlanetCard.vue`：单颗星球展示与选择。
- `src/components/ExplorerControls.vue`：探索入口、册数导航和分页。
- `src/components/ExploreDialog.vue`：探索弹窗，横屏时压缩布局并支持内部滚动。
- `src/composables/usePlanetOrbit.ts`：轨道动画、选中状态、分页和生命周期清理。
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

## 动画参数

`src/config/scene.ts` 统一地球大小、星球基准尺寸、轨道中心、半径和倾角；轨道 SVG 与 `PlanetOrbit.vue` 通过同一个 `orbitPoint()` 函数定位，`styleFor()` 控制纵深及选中放大倍率，`usePlanetOrbit.ts` 中 `ORBIT_SPEED` 控制公转速度。背景地球独立于动画层，不参与旋转或缩放。

原切图已包含册数文字，因此采用图案保持正向的轨道公转，避免旋转整张图片导致文字倒置。若需球体纹理自转，可替换为不含文字的球体素材并单独叠加册数标签。

## 横竖屏适配

场景始终使用 16:9 横向布局，并同时受可用宽、高约束。650px 以下的手机竖屏视口中，`landscape-viewport` 交换可用宽高并旋转 90°，即使微信等内置浏览器锁定竖屏也能呈现横向场景；物理视口转横后自动恢复正常方向。触控由浏览器按 CSS 变换后的实际位置处理，切换方向时不重建组件。

手机紧凑控件同时应用于旋转的竖屏和低高度横屏。原生 `<dialog>` 位于浏览器顶层，单独同步旋转，避免弹窗方向与场景不一致。使用动态视口高度 `100dvh`（回退 `100svh`）及 `env(safe-area-inset-*)` 适配浏览器工具栏和刘海安全区。此方案旋转网页内容，不依赖浏览器的屏幕方向锁定 API。

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
