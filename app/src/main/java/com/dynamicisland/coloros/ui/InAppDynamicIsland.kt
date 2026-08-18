package com.dynamicisland.coloros.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalDensity

/**
 * ============================================================
 * InAppDynamicIsland — 纯 Compose 简化版（应用内灵动岛）
 * ============================================================
 *
 * 适用场景：仅在你的 App 内部使用灵动岛效果
 * 不需要：悬浮窗权限、通知监听权限、前台服务
 *
 * 使用方式：直接在 Activity 的 setContent {} 中调用
 *
 *   InAppDynamicIsland(
 *       title = "新消息",
 *       subtitle = "你有一条微信消息",
 *       isExpanded = true
 *   )
 *
 * 注意：这个版本只能在你自己的 App 界面内显示，
 * 不能像系统级灵动岛那样跨 App 全局显示。
 */
@Composable
fun InAppDynamicIsland(
    title: String = "",
    subtitle: String = "",
    isExpanded: Boolean = false,
    showCameraHole: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 状态栏高度（安全区域）
    val density = LocalDensity.current
    val statusBarHeightPx = WindowInsets.statusBars.getTop(density)
    val statusBarHeightDp = with(density) { statusBarHeightPx.toDp() }

    // 形态动画
    val targetWidth = if (isExpanded) 300.dp else 140.dp
    val targetHeight = if (isExpanded) 120.dp else 38.dp
    val targetRadius = if (isExpanded) 28.dp else 19.dp

    val width by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 350f
        ),
        label = "inAppWidth"
    )
    val height by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 350f
        ),
        label = "inAppHeight"
    )
    val radius by animateDpAsState(
        targetValue = targetRadius,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 350f
        ),
        label = "inAppRadius"
    )

    Box(
        modifier = modifier
            .padding(
                top = statusBarHeightDp + 4.dp,
                start = 66.dp  // Reno7 左上角挖孔右侧
            )
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        // 模拟摄像头挖孔
        if (showCameraHole && !isExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }

        if (isExpanded) {
            // 展开态内容
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // 紧凑态
            if (title.isNotEmpty()) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
