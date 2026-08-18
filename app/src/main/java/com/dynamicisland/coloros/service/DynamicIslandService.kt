package com.dynamicisland.coloros.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import com.dynamicisland.coloros.R
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.ui.DynamicIslandViewModel
import com.dynamicisland.coloros.ui.FluidCloudComposeView

/**
 * ============================================================
 * DynamicIslandService — 全局悬浮窗前台服务
 * ============================================================
 *
 * 为什么必须是前台服务？
 * → ColorOS 13.1 对后台服务有严格限制：
 *   - 后台超过 1 分钟可能被冻结
 *   - 使用前台服务 + 媒体类型，优先级最高
 *   - 配合电池优化白名单可长期保活
 *
 * 为什么用 TYPE_APPLICATION_OVERLAY？
 * → 这是 Android 8.0+ 官方推荐的悬浮窗类型
 * → 替代已废弃的 TYPE_PHONE / TYPE_SYSTEM_ALERT
 * → 需要 SYSTEM_ALERT_WINDOW 权限（用户手动授权）
 *
 * 布局策略：
 * - 使用 WindowManager 在屏幕最顶层添加 ComposeView
 * - Gravity.TOP | Gravity.START 定位到左上角（Reno7 挖孔位置）
 * - 不拦截下方触摸事件（FLAG_NOT_FOCUSABLE 仅在不展开时设置）
 */
class DynamicIslandService : Service() {

    companion object {
        private const val TAG = "DynamicIslandService"
        private const val NOTIFICATION_ID = 10086
        private const val CHANNEL_ID = "dynamic_island_channel"

        /** 启动服务的便捷方法 */
        fun start(context: Context) {
            val intent = Intent(context, DynamicIslandService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** 停止服务 */
        fun stop(context: Context) {
            context.stopService(Intent(context, DynamicIslandService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    // ====== ViewModel（连接状态管理器）======
    private lateinit var viewModel: DynamicIslandViewModel

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 初始化 ViewModel
        val app = application as com.dynamicisland.coloros.DynamicIslandApp
        viewModel = DynamicIslandViewModel(application)

        // 启动前台服务通知
        startForeground(NOTIFICATION_ID, createNotification())

        // 创建悬浮窗
        createOverlay()
    }

    /**
     * 创建前台服务通知
     *
     * ColorOS 13.1 要求：
     * - 必须调用 startForeground() 且在 5 秒内完成
     * - 通知渠道重要性至少 IMPORTANCE_LOW（否则用户会看到持续通知）
     * - 使用 mediaPlayback 类型可隐藏部分通知细节
     */
    private fun createNotification(): Notification {
        // 创建通知渠道（Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "灵动岛服务",
                NotificationManager.IMPORTANCE_LOW  // 不打扰用户
            ).apply {
                description = "保持灵动岛全局显示的必须服务"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }

        // 点击通知 → 打开主 Activity
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.dynamicisland.coloros.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("灵动岛运行中")
            .setContentText("轻触打开设置")
            .setSmallIcon(R.drawable.ic_notification)  // 需提供图标
            .setContentIntent(pendingIntent)
            .setOngoing(true)        // 不可清除
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * 创建悬浮窗（核心方法）
     *
     * 关键参数说明：
     * - TYPE_APPLICATION_OVERLAY：Android 8.0+ 悬浮窗标准类型
     * - FLAG_NOT_FOCUSABLE：默认不抢焦点，让下方 App 可正常操作
     *   → 展开时动态移除该 flag（见 updateFlagsForExpanded）
     * - FLAG_LAYOUT_NO_LIMITS：允许超出状态栏区域
     * - FLAG_HARDWARE_ACCELERATED：开启硬件加速（Compose 必需）
     */
    private fun createOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // 悬浮窗类型（Android 8.0+ 必须用这个）
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 标志位组合：
            // NOT_FOCUSABLE → 不抢焦点
            // NOT_TOUCH_MODAL → 事件穿透到下方
            // LAYOUT_IN_SCREEN → 允许在状态栏区域绘制
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT  // 透明背景
        ).apply {
            // 定位：屏幕顶部，偏左（Reno7 左上角挖孔）
            gravity = Gravity.TOP or Gravity.START
            // 初始位置（实际位置由 Compose 内部 padding 精确控制）
            x = 0
            y = 0
            // 屏幕适配
            layoutInDisplayCutoutMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
                }
        }

        // 创建 ComposeView 并挂载 FluidCloudComposeView
        // 需要手动设置 LifecycleOwner，因为 Service 没有现成的
        val lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.performCreate()

        composeView = ComposeView(this).apply {
            // 设置 ViewCompositionStrategy，确保 detach 时 dispose
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindow
            )
            // 设置 LifecycleOwner（Compose 需要）
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                // 直接渲染核心 Compose 组件
                FluidCloudComposeView(viewModel = viewModel)
            }
        }

        // 添加到 WindowManager
        try {
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            // 常见错误：SYSTEM_ALERT_WINDOW 未授权
            // → 引导用户在 MainActivity 中重新授权
        }
    }

    /**
     * 展开态时移除 NOT_FOCUSABLE，让胶囊可以接收点击
     * 收起时重新加回，避免遮挡下方 App
     */
    private fun updateFlagsForExpanded(isExpanded: Boolean) {
        composeView?.let { view ->
            val currentParams = view.layoutParams as? WindowManager.LayoutParams ?: return
            if (isExpanded) {
                currentParams.flags = currentParams.flags and
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            } else {
                currentParams.flags = currentParams.flags or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            windowManager.updateViewLayout(view, currentParams)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 服务被杀死时自动重启
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // 通知 Lifecycle 销毁
        try {
            (composeView as? ComposeView)?.let {
                // 先 detach → 触发 DisposeOnDetachedFromWindow
                windowManager.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        composeView = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Service 内嵌 LifecycleOwner
     *
     * Compose 需要 LifecycleOwner 来管理重组生命周期。
     * Service 本身不是 LifecycleOwner，所以这里手动实现一个。
     */
    inner class ServiceLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle
            get() = lifecycleRegistry

        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateRegistryController.savedStateRegistry

        fun performCreate() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun performDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }
}
