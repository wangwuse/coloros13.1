# 灵动岛 ColorOS — 高保真仿 iOS Dynamic Island

专为 **OPPO Reno7 5G (ColorOS 13.1 / Android 13)** 优化的全局灵动岛组件。

## ✨ 功能特性

| 功能 | 实现方式 |
|------|---------|
| 全局悬浮窗 | `TYPE_APPLICATION_OVERLAY` + WindowManager |
| 弹簧动画 | Compose `spring(dampingRatio=0.75, stiffness=350)` |
| 三态切换 | Compact / Expanded / Minimal / LongPill |
| 智能反色 | PixelCopy 采样 + BT.601 灰度 + animateColorAsState |
| 毛玻璃模糊 | RenderEffect (Android 12+) |
| 通知捕获 | NotificationListenerService |
| 音乐控制 | MediaSessionManager |
| 充电状态 | BatteryManager |
| 前台保活 | Foreground Service + IMPORTANCE_MIN |
| 电池白名单 | ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS |
| 自启动引导 | ColorOS 自启动管理页面跳转 |
| JobScheduler 复活 | 每 15 分钟尝试拉起主服务 |
| 开机自启 | BOOT_COMPLETED + USER_PRESENT |
| 长按震动 | VibrationEffect |
| 左右滑切换 | detectHorizontalDragGestures |

## 🔧 编译要求

- Android Studio Hedgehog+
- JDK 17
- Gradle 8.5
- compileSdk 34
- minSdk 26

## 📱 安装后设置（ColorOS 必做）

1. **电池优化白名单**：设置 → 电池 → 更多电池设置 → 允许后台活动
2. **自启动**：手机管家 → 权限隐私 → 自启动管理 → 开启本应用
3. **后台锁定**：最近任务卡片下拉 → 锁定
4. **悬浮窗权限**：设置 → 权限管理 → 悬浮窗 → 允许
5. **通知使用权**：设置 → 通知与状态栏 → 通知使用权 → 开启

## 🏗️ 项目结构

```
app/src/main/java/com/dynamicisland/coloros/
├── DynamicIslandApp.kt          # Application 入口
├── helper/
│   ├── ColorOSHelper.kt         # ColorOS 保活工具（电池/自启动/后台）
│   └── PixelSampler.kt         # 背景像素采样（智能反色）
├── manager/
│   ├── IslandStateManager.kt    # 全局状态管理（StateFlow）
│   └── Logcat.kt               # 统一日志工具
├── model/
│   └── IslandModels.kt         # 数据模型（IslandMode / IslandContent）
├── service/
│   ├── DynamicIslandService.kt # 核心前台服务 + 悬浮窗
│   ├── MusicControllerService.kt # 音乐监听 + 空 MediaSession 保活
│   └── NotificationCollectorService.kt # 通知监听
├── receiver/
│   ├── BootAndUnlockReceiver.kt # 开机/解锁拉起
│   ├── BatteryStateReceiver.kt  # 充电状态
│   └── JobResurrectReceiver.kt  # JobScheduler 复活
└── ui/
    ├── MainActivity.kt          # 主界面（权限引导 + 测试面板）
    ├── BatteryWhitelistActivity.kt # 保活设置引导页
    └── IslandUi.kt             # Compose 灵动岛 UI（核心动画）
```

## 📝 调试

Logcat 过滤标签：`tag:DI_*`

关键日志：
- `DI_Service` — 服务生命周期
- `DI_NotifSvc` — 通知监听
- `DI_Music` — 音乐控制
- `DI_Battery` — 充电状态
- `DI_Boot` — 开机/解锁
- `DI_Job` — JobScheduler 复活
- `ColorOS` — 权限检测与跳转
- `PixelSampler` — 背景采样
- `IslandState` — 状态变更

## 🤖 GitHub Actions 自动编译

本项目已配置 GitHub Actions 工作流（`.github/workflows/build.yml`），**无需 `gradlew` 即可编译**：

- 工作流会在每次 push 到 `main` 分支时自动触发
- 也可手动触发：Actions → Build APK → Run workflow
- 编译完成后在 Artifacts 区域下载 `app-debug.apk`

**工作原理**：CI 环境直接安装 Gradle 8.5 二进制，绕过 `gradlew` 和 `gradle-wrapper.jar` 的依赖。

## ⚠️ 已知限制

- ColorOS 极端省电模式下仍可能被限制（需用户手动锁定后台）
- PixelCopy 在部分全屏游戏场景下可能采样失败（fallback 到上次颜色）
- 长按震动在 Android 12+ 需要 VibratorManager 权限