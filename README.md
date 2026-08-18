# 灵动岛 ColorOS — 高保真仿 iOS Dynamic Island

针对 **OPPO Reno7 5G (ColorOS 13.1 / Android 13)** 优化的全局灵动岛组件。

## ✨ 功能特性

| 功能 | 实现方式 |
|------|----------|
| 全局悬浮窗 | WindowManager + TYPE_APPLICATION_OVERLAY |
| 通知捕获 | NotificationListenerService |
| 音乐控制 | MediaSession + MediaController |
| 充电状态 | BatteryManager 监听 |
| 前台保活 | ForegroundService (mediaPlayback 类型) |
| ColorOS 适配 | 电池白名单 + 自启动 + 锁屏后台 |
| 开机自启 | BootReceiver + 解锁检测 |
| 任务堆叠 | 最多5条，左右滑切换 |

## 🔧 编译方式

### GitHub Actions（推荐，零环境配置）

1. 把本项目上传到你的 GitHub 仓库（确保仓库为 **Public**）
2. 进入 **Actions** 标签 → 选择 **Build APK** → 点击 **Run workflow**
3. 等待 5-10 分钟，变绿✅后下载 Artifacts 中的 `app-debug.apk`
4. 安装到手机即可使用

> 💡 提示：网页卡顿时把 `github.com` 换成 `kkgithub.com` 加速

### 本地编译（需 Android Studio）

1. 用 Android Studio 打开本项目
2. 等待 Gradle Sync 完成
3. 点击 Run ▶️ 或执行 `gradle assembleDebug`

## 📱 使用说明

1. 安装 APK 后打开 App
2. 按引导依次开启：
   - ✅ 悬浮窗权限（SYSTEM_ALERT_WINDOW）
   - ✅ 电池白名单（关闭电池优化）
   - ✅ 自启动管理（ColorOS 安全中心）
   - ✅ 通知使用权（通知监听）
3. 点击「🚀 启动灵动岛」
4. 返回桌面，左上角即可看到灵动岛

## ⚠️ ColorOS 13.1 特别注意

- **电池优化**：必须加入白名单，否则锁屏 3 分钟必死
- **自启动管理**：ColorOS 独立管控，需手动开启
- **锁屏后台**：设置 → 电池 → 找到本 App → 允许锁屏后台运行
- **省电模式**：开启后会限制后台，建议关闭或加入例外
- **后台冻结**：设置 → 电池 → 省电模式 → 高级设置 → 排除本应用

## 📋 权限清单

- `SYSTEM_ALERT_WINDOW` — 悬浮窗
- `FOREGROUND_SERVICE` — 前台服务
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — 媒体前台服务
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — 电池白名单
- `POST_NOTIFICATIONS` — 通知推送（Android 13+）
- `BIND_NOTIFICATION_LISTENER_SERVICE` — 通知监听
- `RECEIVE_BOOT_COMPLETED` — 开机自启

## 🏗️ 项目结构

```
app/src/main/java/com/dynamicisland/coloros/
├── DynamicIslandApp.kt              # Application 初始化
├── helper/
│   └── BatteryOptimizationHelper.kt  # ColorOS 电池/自启动权限跳转
├── manager/
│   └── IslandStateManager.kt        # 全局状态管理（单例 + StateFlow）
├── receiver/
│   ├── BatteryReceiver.kt           # 电池/充电监听
│   └── BootReceiver.kt             # 开机/解锁自启
├── service/
│   ├── DynamicIslandService.kt      # 核心前台服务 + 悬浮窗
│   ├── MusicControllerService.kt    # 音乐播放监听与控制
│   └── NotificationCaptureService.kt# 系统通知监听
└── ui/
    ├── MainActivity.kt              # 主界面（权限引导 + 测试面板）
    ├── BatteryWhitelistActivity.kt  # 电池白名单引导页
    ├── DynamicIslandViewModel.kt     # ViewModel（状态 + 事件）
    ├── FluidCloudComposeView.kt      # 核心 Compose UI（胶囊/展开/动画）
    └── InAppDynamicIsland.kt        # 简化版（应用内使用）
```

## 📄 License

MIT License
