package com.dynamicisland.coloros.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.dynamicisland.coloros.R
import com.dynamicisland.coloros.helper.ColorOSHelper
import com.dynamicisland.coloros.helper.PixelSampler
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.ui.IslandUi

/**
 * 灵动岛核心前台服务。
 *
 * 职责：
 * 1. 通过 [WindowManager] 添加 TYPE_APPLICATION_OVERLAY 悬浮窗。
 * 2. 在悬浮窗中承载 ComposeView（[IslandUi]）。
 * 3. 维持前台服务通知，保活进程。
 * 4. 定期采样背景像素，驱动智能反色。
 * 5. 在 onDestroy 中尝试 JobScheduler 复活。
 *
 * 生命周期：
 * - onCreate → 创建通知渠道 + 添加悬浮窗
 * - onStartCommand → startForeground + 调度复活
 * - onDestroy → 移除悬浮窗 + 触发复活
 */
class DynamicIslandService : Service() {

    companion object {
        private const val TAG = "DI_Service"
        private const val NOTIF_CHANNEL_ID = "dynamic_island_foreground"
        private const val NOTIF_ID = 0x4321
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var islandParams: WindowManager.LayoutParams? = null

    // 采样计时器
    private val samplerRunnable = object : Runnable {
        override fun run() {
            composeView?.let { PixelSampler.sampleBackground(windowManager, it) }
            // 每 2 秒采样一次，开销极低
            handler?.postDelayed(this, 2000)
        }
    }
    private var handler: android.os.Handler? = null

    // ──────────────────────────────
    //  生命周期
    // ──────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Logcat.d(TAG, "onCreate — 服务创建")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        handler = android.os.Handler(mainLooper)

        // 1. 创建低优先级通知渠道（不打扰用户）
        createNotificationChannel()

        // 2. 添加悬浮窗
        addIslandOverlay()

        // 3. 启动前台服务
        startForeground(NOTIF_ID, buildNotification())

        // 4. 调度 JobScheduler 复活
        ColorOSHelper.scheduleResurrection(this)

        // 5. 开始背景采样
        handler?.post(samplerRunnable)

        Logcat.i(TAG, "onCreate — 灵动岛已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logcat.d(TAG, "onStartCommand — flags=$flags, startId=$startId")
        // START_STICKY：被杀后系统会尝试重启
        return START_STICKY
    }

    override fun onDestroy() {
        Logcat.w(TAG, "onDestroy — 服务即将销毁，尝试复活")
        super.onDestroy()

        // 移除悬浮窗
        try {
            composeView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Logcat.e(TAG, "removeView failed: ${e.message}")
        }
        composeView = null

        // 停止采样
        handler?.removeCallbacks(samplerRunnable)
        handler = null

        // 触发复活（JobScheduler 会在 15 分钟内拉起）
        ColorOSHelper.scheduleResurrection(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 横竖屏切换时重新定位
        Logcat.d(TAG, "onConfigurationChanged")
        repositionIsland()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ──────────────────────────────
    //  悬浮窗管理
    // ──────────────────────────────

    private fun addIslandOverlay() {
        val ctx = this

        composeView = ComposeView(ctx).apply {
            setContent {
                IslandUi()
            }
        }

        // 计算位置：左上角，紧贴状态栏
        val statusBarH = IslandStateManager.statusBarHeight
        val leftMargin = (8 * resources.displayMetrics.density).toInt() // 8dp

        islandParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            // FLAG_NOT_FOCUSABLE：不抢焦点，不影响下方 App 操作
            // FLAG_LAYOUT_NO_LIMITS：允许绘制到状态栏区域
            // FLAG_HARDWARE_ACCELERATED：强制硬件加速，毛玻璃流畅
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = leftMargin
            y = statusBarH + (4 * resources.displayMetrics.density).toInt() // 4dp 微调
            // 设置较高的 type 数值，确保在大多数 App 之上
            // TYPE_APPLICATION_OVERLAY 范围为 2000-2999，用 2999 确保在最前
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            }
        }

        windowManager.addView(composeView, islandParams)
        Logcat.d(TAG, "addIslandOverlay — 悬浮窗已添加 (statusBar=$statusBarH)")
    }

    private fun repositionIsland() {
        val params = islandParams ?: return
        val statusBarH = IslandStateManager.statusBarHeight
        params.y = statusBarH + (4 * resources.displayMetrics.density).toInt()
        try {
            windowManager.updateViewLayout(composeView, params)
        } catch (e: Exception) {
            Logcat.e(TAG, "reposition failed: ${e.message}")
        }
    }

    // ──────────────────────────────
    //  前台服务通知
    // ──────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "灵动岛后台服务",
                // IMPORTANCE_MIN：最低优先级，不弹通知栏、不发声、不亮屏
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "保持灵动岛常驻运行"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
            Logcat.d(TAG, "NotificationChannel created (IMPORTANCE_MIN)")
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.dynamicisland.coloros.ui.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("灵动岛运行中")
            .setContentText("点击打开设置")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)        // 不可滑动清除
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}