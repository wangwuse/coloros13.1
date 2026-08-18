package com.dynamicisland.coloros.manager

import android.app.Application
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.graphics.toArgb
import com.dynamicisland.coloros.model.IslandMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 灵动岛全局状态管理器（单例）。
 *
 * 职责：
 * 1. 维护当前 [IslandMode]（Compact / Expanded / Minimal / LongPill）。
 * 2. 暴露 [StateFlow] 供 Compose UI 订阅，实现响应式切换。
 * 3. 管理背景色反色结果，驱动 UI 自适应深色/浅色背景。
 * 4. 提供屏幕宽高、状态栏高度等尺寸信息（用于精确放置左上角位置）。
 *
 * 设计要点：
 * - 使用 [MutableStateFlow] 保证线程安全且可被 Compose `collectAsState()` 消费。
 * - 所有状态变更都走 `update()` 内联函数，便于统一加日志。
 */
object IslandStateManager {

    // ───── 公开状态流 ─────

    private val _islandMode = MutableStateFlow(IslandMode.Compact)
    val islandMode: StateFlow<IslandMode> = _islandMode.asStateFlow()

    private val _islandContent = MutableStateFlow(IslandContent.Idle)
    val islandContent: StateFlow<IslandContent> = _islandContent.asStateFlow()

    /** 背景灰度值（0~255），由 PixelCopy 采样后写入 */
    private val _bgLuminance = MutableStateFlow(180)
    val bgLuminance: StateFlow<Int> = _bgLuminance.asStateFlow()

    /** 是否正在充电 */
    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    /** 当前电量 0~100 */
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    // ───── 屏幕尺寸（初始化后可用） ─────

    var screenWidth = 1080
        private set
    var screenHeight = 2400
        private set
    var statusBarHeight = 72
        private set

    // ───── 初始化 ─────

    fun init(app: Application) {
        val wm = app.getSystemService(WindowManager::class.java)
        val display = wm.defaultDisplay
        val size = Point()
        display.getRealSize(size)
        screenWidth = size.x
        screenHeight = size.y

        // 通过资源计算状态栏高度（Reno7 通常为 72~80px）
        val resourceId = app.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = app.resources.getDimensionPixelSize(resourceId)
        }
        Logcat.d("IslandState", "init: ${screenWidth}x$screenHeight, statusBar=$statusBarHeight")
    }

    // ───── 状态变更 API ─────

    fun setMode(mode: IslandMode) {
        Logcat.d("IslandState", "mode → $mode")
        _islandMode.value = mode
    }

    fun setContent(content: IslandContent) {
        Logcat.d("IslandState", "content → $content")
        _islandContent.value = content
    }

    fun setBgLuminance(y: Int) {
        _bgLuminance.value = y.coerceIn(0, 255)
    }

    fun setCharging(charging: Boolean) {
        _isCharging.value = charging
        Logcat.d("IslandState", "charging=$charging")
    }

    fun setBatteryLevel(level: Int) {
        _batteryLevel.value = level.coerceIn(0, 100)
    }

    // ───── 工具：根据亮度决定岛的颜色 ─────
    // Y < 128 → 深色背景 → 岛白色；Y >= 128 → 浅色背景 → 岛黑色

    fun islandBackgroundColor(): androidx.compose.ui.graphics.Color {
        val y = _bgLuminance.value
        return if (y < 128) {
            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.92f)
        } else {
            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.88f)
        }
    }

    fun islandForegroundColor(): androidx.compose.ui.graphics.Color {
        val y = _bgLuminance.value
        return if (y < 128) {
            androidx.compose.ui.graphics.Color.Black
        } else {
            androidx.compose.ui.graphics.Color.White
        }
    }
}