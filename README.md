# 灵动岛 ColorOS — 高保真仿 iOS Dynamic Island

针对 **OPPO Reno7 5G (ColorOS 13.1 / Android 13)** 优化的全局灵动岛组件。

## 📱 功能清单

| 功能 | 实现方式 | 说明 |
|------|----------|------|
| 全局悬浮窗 | `TYPE_APPLICATION_OVERLAY` + 前台服务 | 跨 App 显示 |
| 通知捕获 | `NotificationListenerService` | 外卖/打车/社交 |
| 音乐控制 | `MediaSession` 监听 + 按键指令 | 播放/暂停/上下曲 |
| 充电状态 | `BatteryManager` 广播 | 电量进度 + 预估充满时间 |
| 计时器 | 组件内置 | 圆环进度动画 |
| 电池白名单 | `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | ColorOS 专项适配 |

## 🏗️ 项目结构

```
app/src/main/java/com/dynamicisland/coloros/
├── DynamicIslandApp.kt              # Application 入口
├── manager/
│   └── IslandStateManager.kt        # ⭐ 全局状态管理 (StateFlow)
├── service/
│   ├── DynamicIslandService.kt      # ⭐⭐ 悬浮窗前台服务
│   ├── NotificationCaptureService.kt# 通知监听 → 灵动岛数据
│   └── MusicControllerService.kt   # MediaSession 音乐控制
├── receiver/
│   └── BatteryReceiver.kt          # 充电/电量广播
├── helper/
│   └── BatteryOptimizationHelper.kt # ColorOS 电池白名单引导
└── ui/
    ├── MainActivity.kt             # 权限引导 + 测试面板
    ├── DynamicIslandViewModel.kt    # ViewModel + 交互事件
    ├── FluidCloudComposeView.kt     # ⭐⭐⭐ 核心 Compose UI
    └── InAppDynamicIsland.kt       # 纯 Compose 简化版（无需权限）
```

## 🚀 编译运行

### 环境要求
- **Android Studio** Hedgehog+
- **JDK** 17
- **Gradle** 8.5（wrapper 自动下载）
- **Kotlin** 1.9.22
- **Compose Compiler** 1.5.8
- **targetSdk** 33
- **测试设备**：OPPO Reno7 5G / ColorOS 13.1（或其他 ColorOS 设备）

### 步骤

1. **解压项目** → 用 Android Studio 打开 `DynamicIslandColorOS` 文件夹
2. **等待 Gradle Sync** 完成（首次约 2-5 分钟，自动下载依赖）
3. **连接手机**：USB 调试开启，确认 `adb devices` 能看到设备
4. **点击 Run ▶️** 安装到手机

### 首次使用配置

App 启动后会进入权限引导页，需要依次开启 **5 个权限**：

| # | 权限 | 路径 | 必须？ |
|---|------|------|--------|
| 1 | 通知权限 | App 内弹窗授权 | ✅ Android 13+ 必需 |
| 2 | 悬浮窗权限 | 设置→权限与隐私→悬浮窗 | ✅ 全局胶囊必需 |
| 3 | 通知使用权 | 设置→通知→通知使用权 | ✅ 抓取他 App 通知 |
| 4 | 电池白名单 | 设置→电池→更多→不优化 | ⚠️ ColorOS 保活关键 |
| 5 | 自启动 | 设置→权限与隐私→自启动 | ⚠️ 防止被杀 |

> 💡 **权限 4 和 5 是 ColorOS 上能否长期运行的关键！**
> 不设置的话，锁屏 3-5 分钟后服务会被冻结。

## 🎨 架构设计

### 数据流

```
┌─────────────────────────────────────────────────────┐
│                  IslandStateManager                  │
│              (单例 + StateFlow + 堆叠)               │
└────────┬───────────────┬───────────────┬────────────┘
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ Service │    │Receiver │    │  Activity│
    │(悬浮窗) │    │(电池)   │    │(权限引导)│
    └─────────┘    └─────────┘    └─────────┘
         │               │               │
         └───────────────┴───────────────┘
                         │
                  ┌──────▼──────┐
                  │  ViewModel  │
                  └──────┬──────┘
                         │
                  ┌──────▼──────┐
                  │ Compose UI  │
                  │(弹簧动画)    │
                  └─────────────┘
```

### 三种形态切换

| 形态 | 尺寸 | 圆角 | 用途 |
|------|------|------|------|
| COMPACT | 130×40dp | 20dp | 默认胶囊，单行文字 |
| EXPANDED | 320×160dp | 28dp | 展开卡片，富内容+控件 |
| MINIMAL | 50×36dp | 18dp | 仅图标，省空间 |
| LONG_PILL | 260×48dp | 24dp | 充电/计时器长条 |

### 弹簧动画参数

```kotlin
spring(
    dampingRatio = 0.8f,    // 接近临界阻尼，轻微过冲
    stiffness = 350f        // 中等刚度，约 0.35s 完成
)
```

> 参考 iOS 灵动岛的 timing curve，在 120Hz 屏（Reno7 支持）上非常丝滑。

## ⚠️ ColorOS 13.1 避坑指南

### 1. 后台被杀
**现象**：锁屏后 3 分钟灵动岛消失  
**解决**：引导用户加入电池优化白名单 + 自启动管理  
**代码**：`BatteryOptimizationHelper.kt`

### 2. 通知监听失效
**现象**：通知使用权已开启，但收不到通知  
**原因**：ColorOS 可能单独限制了通知监听后台运行  
**解决**：同样加入电池白名单 + 锁定后台（多任务卡片下拉加锁）

### 3. 悬浮窗被遮挡
**现象**：灵动岛出现在状态栏下方但被系统 UI 遮挡  
**解决**：使用 `TYPE_APPLICATION_OVERLAY`（系统会自动置顶）  
**注意**：不要使用已废弃的 `TYPE_PHONE`

### 4. RenderEffect 性能
**现象**：开启模糊后高刷掉帧  
**解决**：blurRadius 控制在 16-20 之间，避免在低电量模式开启  
**降级**：Android 12 以下自动降级为纯色半透明

### 5. Reno7 挖孔位置
- 左上角单挖孔，直径约 38dp
- 胶囊左边缘 = 挖孔左边缘(24dp) + 挖孔直径(38dp) + 间距(4dp) = 66dp
- 垂直居中于状态栏图标

## 📄 License

MIT License
