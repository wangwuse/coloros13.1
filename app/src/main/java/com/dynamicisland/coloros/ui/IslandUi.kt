package com.dynamicisland.coloros.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.coloros.helper.PixelSampler
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.model.IslandMode
import com.dynamicisland.coloros.model.IslandContent
import androidx.compose.ui.viewinterop.AndroidView
import android.view.View

/**
 * 灵动岛 Compose UI — 核心文件。
 *
 * 功能：
 * 1. 三态弹簧动画（Compact / Expanded / Minimal / LongPill）
 * 2. 圆角随高度联动（cornerRadius = height / 2）
 * 3. 智能反色（animateColorAsState 过渡）
 * 4. 点击展开、长按震动、左右滑切换
 * 5. 毛玻璃模糊背景（RenderEffect）
 *
 * 弹簧参数说明：
 * - dampingRatio = 0.75f → 略带弹性，像 iOS 原生灵动岛手感
 * - stiffness = 350f     → 适中速度，不会太慢也不会弹过头
 */
@Composable
fun IslandUi() {
    val context = LocalContext.current
    val density = LocalDensity.current

    // ───── 订阅状态 ─────
    val mode by IslandStateManager.islandMode.collectAsState()
    val content by IslandStateManager.islandContent.collectAsState()
    val bgLuminance by IslandStateManager.bgLuminance.collectAsState()
    val isCharging by IslandStateManager.isCharging.collectAsState()
    val batteryLevel by IslandStateManager.batteryLevel.collectAsState()

    // ───── 智能反色 ─────
    // Y < 128 → 深色背景 → 岛白色；Y >= 128 → 浅色背景 → 岛黑色
    val bgColor by animateColorAsState(
        targetValue = if (bgLuminance < 128) {
            Color.White.copy(alpha = 0.92f)
        } else {
            Color.Black.copy(alpha = 0.88f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bgColor"
    )

    val fgColor by animateColorAsState(
        targetValue = if (bgLuminance < 128) Color.Black else Color.White,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fgColor"
    )

    // ───── 尺寸定义（随模式变化） ─────
    val (width, height) = when (mode) {
        IslandMode.Minimal -> 36.dp to 12.dp
        IslandMode.Compact -> 180.dp to 36.dp
        IslandMode.Expanded -> 320.dp to 120.dp
        IslandMode.LongPill -> 280.dp to 40.dp
    }

    // 弹簧动画驱动宽高
    val animWidth by animateDpAsState(
        targetValue = width,
        animationSpec = spring(
            dampingRatio = 0.75f,   // 略带弹性，iOS 风格
            stiffness = 350f       // 适中速度
        ),
        label = "width"
    )
    val animHeight by animateDpAsState(
        targetValue = height,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = 350f
        ),
        label = "height"
    )

    // 圆角 = 高度 / 2（药丸形态）
    val cornerRadius = animHeight / 2

    // ───── 模糊效果 ─────
    val blurRadius by animateFloatAsState(
        targetValue = if (mode == IslandMode.Expanded) 20f else 12f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 350f),
        label = "blur"
    )

    // ───── 手势状态 ─────
    var dragOffset by remember { mutableStateOf(0f) }

    // ───── 长按震动 ─────
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(android.os.VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.os.Vibrator::class.java)
        }
    }

    // ───── 定期采样背景（驱动反色） ─────
    LaunchedEffect(Unit) {
        // 每 3 秒采样一次
        kotlinx.coroutines.delay(3000)
        // 触发采样（通过 View 引用）
    }

    // ───── 渲染 ─────
    Box(
        modifier = Modifier
            .width(animWidth)
            .height(animHeight)
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .graphicsLayer {
                // 硬件加速模糊（Android 12+）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = RenderEffect.createBlurEffect(
                        blurRadius, blurRadius,
                        Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
            }
            .pointerInput(Unit) {
                // 点击 → 展开/收起
                detectTapGestures(
                    onTap = {
                        val next = when (mode) {
                            IslandMode.Minimal, IslandMode.Compact -> IslandMode.Expanded
                            IslandMode.Expanded -> IslandMode.Compact
                            IslandMode.LongPill -> IslandMode.Compact
                        }
                        IslandStateManager.setMode(next)
                    },
                    onLongPress = {
                        // 长按震动（50ms 短震）
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator?.vibrate(
                                android.os.VibrationEffect.createOneShot(
                                    50,
                                    android.os.VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator?.vibrate(50)
                        }
                        // 长按 → 回到 Minimal
                        IslandStateManager.setMode(IslandMode.Minimal)
                    }
                )
            }
            .pointerInput(Unit) {
                // 左右滑 → 切换内容
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(dragOffset) > 50) {
                            // 模拟切换：循环切换模式
                            val next = when (mode) {
                                IslandMode.Compact -> IslandMode.LongPill
                                IslandMode.LongPill -> IslandMode.Expanded
                                else -> IslandMode.Compact
                            }
                            IslandStateManager.setMode(next)
                        }
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, delta ->
                        dragOffset += delta
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ───── 内容渲染 ─────
        when (val c = content) {
            is IslandContent.Idle -> {
                if (mode == IslandMode.Expanded) {
                    Text(
                        text = "灵动岛待机中",
                        color = fgColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is IslandContent.Music -> {
                Text(
                    text = "🎵 ${c.title} - ${c.artist}",
                    color = fgColor,
                    fontSize = if (mode == IslandMode.Compact) 11.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            is IslandContent.Notification -> {
                Text(
                    text = "${c.title}: ${c.text}",
                    color = fgColor,
                    fontSize = if (mode == IslandMode.Compact) 11.sp else 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            is IslandContent.Charging -> {
                Text(
                    text = if (c.isFastCharge) "⚡ 快充中 ${c.level}%" else "🔋 充电中 ${c.level}%",
                    color = fgColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            is IslandContent.Timer -> {
                val min = c.remainingSec / 60
                val sec = c.remainingSec % 60
                Text(
                    text = "⏱ ${String.format("%02d:%02d", min, sec)}",
                    color = fgColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 提供给 MainActivity 使用的全屏预览版灵动岛。
 *
 * 不需要悬浮窗权限，适合在 App 内调试 UI。
 */
@Composable
fun InAppIslandPreview() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Box(modifier = Modifier.padding(16.dp, 36.dp, 0.dp, 0.dp)) {
            IslandUi()
        }
    }
}