package com.dynamicisland.coloros.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dynamicisland.coloros.helper.BatteryOptimizationHelper
import com.dynamicisland.coloros.service.DynamicIslandService

/**
 * ============================================================
 * MainActivity — 权限引导 + 测试面板
 * ============================================================
 *
 * 功能：
 * 1. 检查并引导开启所有必要权限
 * 2. 提供测试按钮（模拟通知/音乐/充电/计时器）
 * 3. 启动/停止灵动岛服务
 *
 * 设计风格：Material 3 + 深色主题
 */
class MainActivity : ComponentActivity() {

    // ====== 权限请求 Launcher ======
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 回调：权限授予结果
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* 悬浮窗权限回调 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF1DB954),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E)
                )
            ) {
                MainScreen(
                    onRequestNotificationPermission = { requestNotificationPermission() },
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onOpenNotificationListenerSettings = { openNotificationListenerSettings() },
                    onOpenBatterySettings = { openBatterySettings() },
                    onStartIsland = { startIslandService() },
                    onStopIsland = { stopIslandService() }
                )
            }
        }
    }

    /**
     * 请求 Android 13+ 通知权限
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * 请求悬浮窗权限（SYSTEM_ALERT_WINDOW）
     *
     * 注意：这不是普通运行时权限，需要跳转到系统设置页
     */
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }
    }

    /**
     * 打开通知使用权设置页
     */
    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /**
     * 打开电池优化设置
     */
    private fun openBatterySettings() {
        val helper = BatteryOptimizationHelper(this)
        val intent = helper.getWhitelistIntent()
        startActivity(intent)
    }

    /**
     * 启动灵动岛服务
     */
    private fun startIslandService() {
        DynamicIslandService.start(this)
    }

    /**
     * 停止灵动岛服务
     */
    private fun stopIslandService() {
        DynamicIslandService.stop(this)
    }
}

/**
 * 主界面 Compose 布局
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    onRequestNotificationPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onStartIsland: () -> Unit,
    onStopIsland: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DynamicIslandViewModel = viewModel(
        factory = DynamicIslandViewModelFactory(context.applicationContext as android.app.Application)
    )

    // 权限状态
    var hasNotificationPerm by remember { mutableStateOf(false) }
    var hasOverlayPerm by remember { mutableStateOf(false) }

    // 检查权限
    LaunchedEffect(Unit) {
        hasNotificationPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        hasOverlayPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "灵动岛 (ColorOS 13.1)",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1E1E1E)
                )
            )
        },
        containerColor = Color(0xFF121212)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ====== 权限状态卡片 ======
            Text(
                "权限设置",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            PermissionCard(
                title = "通知权限",
                description = "Android 13+ 必需，用于接收系统通知",
                isGranted = hasNotificationPerm,
                onClick = onRequestNotificationPermission
            )

            PermissionCard(
                title = "悬浮窗权限",
                description = "全局灵动岛必需，允许在屏幕顶层绘制",
                isGranted = hasOverlayPerm,
                onClick = onRequestOverlayPermission
            )

            PermissionCard(
                title = "通知使用权",
                description = "读取其他 App 的通知内容（外卖/打车/音乐）",
                isGranted = false,  // 需要实际检查
                onClick = onOpenNotificationListenerSettings
            )

            PermissionCard(
                title = "电池优化白名单",
                description = "ColorOS 必需！防止后台被杀",
                isGranted = false,
                onClick = onOpenBatterySettings
            )

            Spacer(Modifier.height(8.dp))

            // ====== 服务控制 ======
            Text(
                "灵动岛控制",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartIsland,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954)
                    )
                ) {
                    Text("🚀 启动灵动岛", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onStopIsland,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⏹ 停止")
                }
            }

            Spacer(Modifier.height(8.dp))

            // ====== 测试面板 ======
            Text(
                "测试面板",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                "点击下方按钮模拟不同类型的内容：",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestButton("📱 通知", { viewModel.pushTestNotification() })
                TestButton("🎵 音乐", { viewModel.pushTestMusic() })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TestButton("⚡ 充电", { viewModel.pushTestCharging() })
                TestButton("⏱ 计时", { viewModel.pushTestTimer() })
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { viewModel.clearAll() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                )
            ) {
                Text("🗑 清除所有")
            }

            Spacer(Modifier.height(16.dp))

            // ====== ColorOS 适配说明 ======
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2A2A2A)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "📋 ColorOS 13.1 适配说明",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        """
                        1. 本应用使用前台服务 + 媒体类型保持常驻
                        2. 必须加入电池白名单，否则锁屏3分钟后被冻结
                        3. 通知使用权需手动开启（系统限制）
                        4. 悬浮窗权限需手动授权
                        5. 部分音乐App可能不标准实现MediaSession
                        6. Reno7 左上角挖孔已适配，胶囊自动避开
                        """.trimIndent(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0xFF1B5E20) else Color(0xFF2A2A2A)
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            }
            Text(
                if (isGranted) "✅" else "⚠️",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun TestButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF333333)
        )
    ) {
        Text(text, fontSize = 13.sp)
    }
}

/**
 * ViewModel Factory — 因为 DynamicIslandViewModel 需要 Application 参数
 */
class DynamicIslandViewModelFactory(
    private val application: android.app.Application
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DynamicIslandViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DynamicIslandViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
