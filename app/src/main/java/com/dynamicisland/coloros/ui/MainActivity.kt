package com.dynamicisland.coloros.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dynamicisland.coloros.R
import com.dynamicisland.coloros.helper.ColorOSHelper
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.model.IslandContent
import com.dynamicisland.coloros.model.IslandMode
import com.dynamicisland.coloros.service.DynamicIslandService
import com.dynamicisland.coloros.service.MusicControllerService

/**
 * 主界面。
 *
 * 功能：
 * 1. 权限引导卡片（悬浮窗/通知/电池/自启动）
 * 2. 测试面板（模拟音乐/通知/充电/计时器）
 * 3. 启动/停止灵动岛服务
 */
class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkAndRefresh() }

    private val batteryPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { checkAndRefresh() }

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { checkAndRefresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logcat.d("MainActivity", "onCreate")

        // 首次启动：跳转权限引导页
        // 简化：直接在主界面展示所有引导

        setContent {
            MainScreen(
                onRequestOverlay = {
                    ColorOSHelper.requestOverlayPermission(this, 1002)
                },
                onRequestBattery = {
                    ColorOSHelper.requestBatteryWhitelist(this, 1001)
                },
                onRequestAutoStart = {
                    ColorOSHelper.requestAutoStart(this)
                },
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    ColorOSHelper.requestNotificationAccess(this, 1003)
                },
                onStartIsland = { startIslandService() },
                onStopIsland = { stopIslandService() },
                onTestMusic = {
                    IslandStateManager.setContent(
                        IslandContent.Music("Test Song", "Dynamic Island", true)
                    )
                    IslandStateManager.setMode(IslandMode.Compact)
                },
                onTestNotification = {
                    IslandStateManager.setContent(
                        IslandContent.Notification("外卖", "骑手已接单，预计 15 分钟送达", "meituan")
                    )
                    IslandStateManager.setMode(IslandMode.Expanded)
                },
                onTestCharging = {
                    IslandStateManager.setCharging(true)
                    IslandStateManager.setBatteryLevel(78)
                    IslandStateManager.setContent(
                        IslandContent.Charging(level = 78, isFastCharge = true)
                    )
                    IslandStateManager.setMode(IslandMode.LongPill)
                },
                onTestTimer = {
                    IslandStateManager.setContent(IslandContent.Timer(remainingSec = 300))
                    IslandStateManager.setMode(IslandMode.Compact)
                },
                onClear = {
                    IslandStateManager.setMode(IslandMode.Compact)
                    IslandStateManager.setContent(IslandContent.Idle)
                    IslandStateManager.setCharging(false)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        checkAndRefresh()
    }

    private fun checkAndRefresh() {
        // 刷新权限状态（触发重组）
        setContent {
            MainScreen(
                onRequestOverlay = { ColorOSHelper.requestOverlayPermission(this, 1002) },
                onRequestBattery = { ColorOSHelper.requestBatteryWhitelist(this, 1001) },
                onRequestAutoStart = { ColorOSHelper.requestAutoStart(this) },
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    ColorOSHelper.requestNotificationAccess(this, 1003)
                },
                onStartIsland = { startIslandService() },
                onStopIsland = { stopIslandService() },
                onTestMusic = {
                    IslandStateManager.setContent(
                        IslandContent.Music("Test Song", "Dynamic Island", true)
                    )
                    IslandStateManager.setMode(IslandMode.Compact)
                },
                onTestNotification = {
                    IslandStateManager.setContent(
                        IslandContent.Notification("外卖", "骑手已接单，预计 15 分钟送达", "meituan")
                    )
                    IslandStateManager.setMode(IslandMode.Expanded)
                },
                onTestCharging = {
                    IslandStateManager.setCharging(true)
                    IslandStateManager.setBatteryLevel(78)
                    IslandStateManager.setContent(
                        IslandContent.Charging(level = 78, isFastCharge = true)
                    )
                    IslandStateManager.setMode(IslandMode.LongPill)
                },
                onTestTimer = {
                    IslandStateManager.setContent(IslandContent.Timer(remainingSec = 300))
                    IslandStateManager.setMode(IslandMode.Compact)
                },
                onClear = {
                    IslandStateManager.setMode(IslandMode.Compact)
                    IslandStateManager.setContent(IslandContent.Idle)
                    IslandStateManager.setCharging(false)
                }
            )
        }
    }

    private fun startIslandService() {
        val intent = Intent(this, DynamicIslandService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // 同时启动音乐监听
        val musicIntent = Intent(this, MusicControllerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(musicIntent)
        } else {
            startService(musicIntent)
        }
        Logcat.i("MainActivity", "灵动岛服务已启动")
    }

    private fun stopIslandService() {
        stopService(Intent(this, DynamicIslandService::class.java))
        stopService(Intent(this, MusicControllerService::class.java))
        ColorOSHelper.cancelResurrection(this)
        Logcat.i("MainActivity", "灵动岛服务已停止")
    }
}

// ──────────────────────────────
//  Compose UI
// ──────────────────────────────

@Composable
private fun MainScreen(
    onRequestOverlay: () -> Unit,
    onRequestBattery: () -> Unit,
    onRequestAutoStart: () -> Unit,
    onRequestNotification: () -> Unit,
    onStartIsland: () -> Unit,
    onStopIsland: () -> Unit,
    onTestMusic: () -> Unit,
    onTestNotification: () -> Unit,
    onTestCharging: () -> Unit,
    onTestTimer: () -> Unit,
    onClear: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // 权限状态
    var hasOverlay by remember {
        mutableStateOf(ColorOSHelper.hasOverlayPermission(context))
    }
    var hasBattery by remember {
        mutableStateOf(ColorOSHelper.isBatteryWhitelisted(context))
    }
    var hasNotif by remember {
        mutableStateOf(ColorOSHelper.hasNotificationAccess(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // ── 标题 ──
        Text(
            text = "灵动岛 ColorOS",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "OPPO Reno7 5G · ColorOS 13.1 · Android 13",
            color = Color.Gray,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(20.dp))

        // ── 权限引导卡片 ──
        Text("权限设置", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        PermissionCard(
            title = "悬浮窗权限",
            desc = "允许灵动岛在任意界面显示",
            granted = hasOverlay,
            onRequest = { onRequestOverlay() }
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            title = "电池优化白名单",
            desc = "关闭电池优化，防止锁屏被杀",
            granted = hasBattery,
            onRequest = { onRequestBattery() }
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            title = "自启动 + 后台运行",
            desc = "ColorOS 特有，允许锁屏后台活动",
            granted = false, // 无法自动检测 ColorOS 自启动状态
            onRequest = { onRequestAutoStart() }
        )
        Spacer(Modifier.height(8.dp))
        PermissionCard(
            title = "通知使用权",
            desc = "读取外卖/打车/音乐通知",
            granted = hasNotif,
            onRequest = { onRequestNotification() }
        )

        Spacer(Modifier.height(24.dp))

        // ── 服务控制 ──
        Text("服务控制", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row {
            Button(
                onClick = onStartIsland,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) { Text("启动灵动岛", color = Color.White) }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onStopIsland,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
            ) { Text("停止服务", color = Color.White) }
        }

        Spacer(Modifier.height(24.dp))

        // ── 测试面板 ──
        Text("测试面板", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = onTestMusic) { Text("🎵 音乐") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onTestNotification) { Text("📦 外卖") }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Button(onClick = onTestCharging) { Text("⚡ 充电") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onTestTimer) { Text("⏱ 计时") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) { Text("清除") }
        }

        Spacer(Modifier.height(24.dp))

        // ── 使用提示 ──
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("保活设置提示", color = Color.Yellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "1. 电池 → 更多电池设置 → 允许后台活动\n" +
                    "2. 手机管家 → 权限隐私 → 自启动管理 → 开启\n" +
                    "3. 最近任务卡片下拉 → 锁定后台\n" +
                    "4. 设置 → 电池 → 本应用 → 不优化",
                    color = Color.LightGray, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    desc: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (granted) Color(0xFF1B5E20) else Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(desc, color = Color.Gray, fontSize = 11.sp)
            }
            if (granted) {
                Text("✅ 已授权", color = Color(0xFF4CAF50), fontSize = 12.sp)
            } else {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("去开启", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}