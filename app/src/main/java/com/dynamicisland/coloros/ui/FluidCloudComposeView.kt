package com.dynamicisland.coloros.ui

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalContext
import com.dynamicisland.coloros.manager.IslandContentType
import com.dynamicisland.coloros.manager.IslandData
import com.dynamicisland.coloros.manager.IslandMorph
import kotlin.math.abs

/**
 * ============================================================
 * FluidCloudComposeView — 仿灵动岛核心 Compose 组件
 * ============================================================
 *
 * 设计参考：
 * - iOS Dynamic Island 的三种形态（Compact / Expanded / Minimal）
 * - ColorOS 13.1「流体云」的交互逻辑（点击展开、长按详情、左右滑切换）
 *
 * 视觉要点：
 * 1. 药丸形态：圆角 = 高度/2（完美半圆端帽）
 * 2. 深色半透明背景 + backdrop blur（RenderEffect）
 * 3. 左侧模拟前置摄像头黑点（Reno7 左上角挖孔）
 * 4. 入场动画：scale + alpha 弹性
 * 5. 形态切换：spring 弹簧（dampingRatio=0.8, stiffness=350）
 *
 * 性能要点：
 * - 所有动画使用 Compose 原生 animate*AsState（硬件加速）
 * - RenderEffect 仅在 API 31+ 启用，低版本降级为纯色半透明
 * - 避免不必要的重组：data 用 derivedStateOf 缓存
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun FluidCloudComposeView(
    viewModel: DynamicIslandViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // ====== 从 ViewModel 收集状态 ======
    val data by viewModel.currentData.collectAsState()
    val morph by viewModel.morph.collectAsState()
    val isVisible by viewModel.isVisible.collectAsState()
    val stack by viewModel.stack.collectAsState()
    val stackIndex by viewModel.stackIndex.collectAsState()

    // ====== 状态栏高度（用于安全区域偏移）======
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val statusBarHeightDp = with(density) { statusBarHeightPx.toDp() }

    // ====== Reno7 左上角挖孔适配 ======
    // Reno7 挖孔约 直径 38dp，位于屏幕左上角 (约 x=24dp, y=居中于状态栏)
    // 胶囊左边缘紧贴挖孔右侧 + 4dp 间距
    val holePunchDiameter = 38.dp
    val holePunchXOffset = 24.dp   // 挖孔左边缘距屏幕左边缘
    val capsuleLeftMargin = holePunchXOffset + holePunchDiameter + 4.dp

    // ====== 入场/退场动画 ======
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.7f,    // 轻微过冲
                        stiffness = 350f
                    ),
                    initialOffsetY = { -it / 2 }  // 从上方滑入
                ),
        exit = fadeOut(animationSpec = tween(150)) +
               slideOutVertically(
                   animationSpec = tween(200),
                   targetOffsetY = { -it }
               )
    ) {
        // ====== 形态驱动的尺寸动画 ======
        // 弹簧参数参考 iOS 灵动岛：dampingRatio ≈ 0.8, stiffness ≈ 350
        val capsuleWidth by animateDpAsState(
            targetValue = when (morph) {
                IslandMorph.COMPACT  -> 130.dp
                IslandMorph.EXPANDED -> 320.dp
                IslandMorph.MINIMAL  -> 50.dp
                IslandMorph.LONG_PILL -> 260.dp
            },
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 350f
            ),
            label = "capsuleWidth"
        )

        val capsuleHeight by animateDpAsState(
            targetValue = when (morph) {
                IslandMorph.COMPACT  -> 40.dp
                IslandMorph.EXPANDED -> 160.dp
                IslandMorph.MINIMAL  -> 36.dp
                IslandMorph.LONG_PILL -> 48.dp
            },
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 350f
            ),
            label = "capsuleHeight"
        )

        val cornerRadius by animateDpAsState(
            targetValue = when (morph) {
                IslandMorph.COMPACT  -> 20.dp   // 高度/2 = 完美药丸
                IslandMorph.EXPANDED -> 28.dp   // 大卡片圆角
                IslandMorph.MINIMAL  -> 18.dp
                IslandMorph.LONG_PILL -> 24.dp
            },
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 350f
            ),
            label = "cornerRadius"
        )

        // ====== 内容 Alpha 动画（展开时淡入内容）======
        val contentAlpha by animateFloatAsState(
            targetValue = if (morph == IslandMorph.EXPANDED) 1f else 0f,
            animationSpec = tween(durationMillis = 200, delayMillis = 100),
            label = "contentAlpha"
        )

        // ====== 模糊效果（仅 API 31+）======
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.graphicsLayer {
                // 模拟 backdrop blur
                // 注意：RenderEffect 性能开销较大，ColorOS 上建议 blurRadius ≤ 20
                renderEffect = RenderEffect.createBlurEffect(
                    16f, 16f,
                    Shader.TileMode.CLAMP
                )
            }
        } else {
            Modifier
        }

        // ====== 拖动手势状态 ======
        var dragOffsetX by remember { mutableStateOf(0f) }

        // ====== 主容器 ======
        Box(
            modifier = modifier
                .padding(
                    start = capsuleLeftMargin,
                    top = statusBarHeightDp + 4.dp  // 状态栏下方 4dp
                )
                .width(capsuleWidth)
                .height(capsuleHeight)
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    color = Color(0xCC000000),  // 深色半透明 80%
                    shape = RoundedCornerShape(cornerRadius)
                )
                .then(blurModifier)
                // ====== 点击交互 ======
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { viewModel.onCapsuleClick() },
                        onLongPress = { viewModel.onCapsuleLongPress() }
                    )
                }
                // ====== 水平滑动（切换堆叠任务）=====
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            dragOffsetX += dragAmount
                            change.consume()
                        },
                        onDragEnd = {
                            if (abs(dragOffsetX) > 60) {
                                if (dragOffsetX > 0) viewModel.onSwipeRight()
                                else viewModel.onSwipeLeft()
                            }
                            dragOffsetX = 0f
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // ====== 左侧模拟摄像头挖孔黑点 ======
            // 仅在 Compact 和 Expanded 形态显示
            if (morph != IslandMorph.MINIMAL) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 6.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                )
            }

            // ====== 根据形态渲染不同内容 ======
            when (morph) {
                IslandMorph.COMPACT -> CompactContent(data = data)
                IslandMorph.EXPANDED -> ExpandedContent(
                    data = data,
                    alpha = contentAlpha,
                    onPlayPause = {
                        MusicControllerService.sendAction(context, "action_play_pause")
                    },
                    onNext = {
                        MusicControllerService.sendAction(context, "action_next")
                    },
                    onPrev = {
                        MusicControllerService.sendAction(context, "action_prev")
                    }
                )
                IslandMorph.MINIMAL -> MinimalContent(data = data)
                IslandMorph.LONG_PILL -> LongPillContent(data = data)
            }

            // ====== 堆叠指示器（右上角小圆点）======
            if (stack.size > 1) {
                StackIndicator(
                    currentIndex = stackIndex,
                    total = stack.size,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 8.dp)
                )
            }
        }
    }
}

// ============================================================
// Compact 形态 — 小胶囊，单行文字 + 图标
// ============================================================
@Composable
private fun CompactContent(data: IslandData) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 根据类型显示对应图标
        val iconVector = when (data.contentType) {
            IslandContentType.MUSIC -> Icons.Filled.PlayArrow
            IslandContentType.CHARGING -> Icons.Filled.PlayArrow // 可替换为充电图标
            else -> null
        }
        iconVector?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }

        // 单行文字（自动省略）
        Text(
            text = when (data.contentType) {
                IslandContentType.IDLE -> "灵动岛"
                IslandContentType.MUSIC -> "♪ ${data.title}"
                IslandContentType.CHARGING -> "⚡ 充电中"
                IslandContentType.TIMER -> "⏱ ${data.subtitle}"
                IslandContentType.NAVIGATION -> "🧭 ${data.title}"
                IslandContentType.NOTIFICATION -> data.title
            },
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================
// Expanded 形态 — 大卡片，富内容
// ============================================================
@Composable
private fun ExpandedContent(
    data: IslandData,
    alpha: Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 来源 App 名称
        Text(
            text = data.title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer { this.alpha = alpha }
        )

        Spacer(Modifier.height(4.dp))

        // 副标题 / 详情
        if (data.subtitle.isNotEmpty()) {
            Text(
                text = data.subtitle,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }

        // 正文
        if (data.body.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = data.body,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }

        // ====== 音乐控制栏 ======
        if (data.contentType == IslandContentType.MUSIC) {
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            ) {
                // 上一曲
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "上一首",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onPrev() })
                        }
                )
                // 播放/暂停
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "播放/暂停",
                    tint = Color.White,
                    modifier = Modifier
                        .size(36.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onPlayPause() })
                        }
                )
                // 下一曲
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "下一首",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onNext() })
                        }
                )
            }

            // 进度条
            Spacer(Modifier.height(8.dp))
            MusicProgressBar(progress = data.progress, alpha = alpha)
        }
    }
}

// ============================================================
// Minimal 形态 — 仅小图标
// ============================================================
@Composable
private fun MinimalContent(data: IslandData) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (data.contentType) {
                IslandContentType.MUSIC -> "♪"
                IslandContentType.CHARGING -> "⚡"
                IslandContentType.NAVIGATION -> "→"
                else -> "●"
            },
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

// ============================================================
// Long Pill 形态 — 长条状（充电进度 / 计时器圆环）
// ============================================================
@Composable
private fun LongPillContent(data: IslandData) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧图标
        Text(
            text = if (data.contentType == IslandContentType.CHARGING) "⚡" else "⏱",
            fontSize = 18.sp
        )

        Spacer(Modifier.width(10.dp))

        // 中间文字
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (data.subtitle.isNotEmpty()) {
                Text(
                    text = data.subtitle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }

        // 右侧进度环（Canvas 绘制）
        ProgressRing(progress = data.progress)
    }
}

// ============================================================
// 进度环（Canvas 绘制，120Hz 友好）
// ============================================================
@Composable
private fun ProgressRing(progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progressRing"
    )

    Canvas(
        modifier = Modifier.size(28.dp),
        onDraw = {
            // 底色环
            drawArc(
                color = Color.White.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            // 进度环
            drawArc(
                color = Color(0xFF4CAF50),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    )
}

// ============================================================
// 音乐进度条
// ============================================================
@Composable
private fun MusicProgressBar(progress: Float, alpha: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "musicProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color.White.copy(alpha = 0.2f * alpha))
            .graphicsLayer { this.alpha = alpha }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(Color(0xFF1DB954))  // Spotify 绿
        )
    }
}

// ============================================================
// 堆叠指示器（右上角小圆点）
// ============================================================
@Composable
private fun StackIndicator(
    currentIndex: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentIndex) Color.White
                        else Color.White.copy(alpha = 0.3f)
                    )
            )
        }
    }
}
