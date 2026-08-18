package com.dynamicisland.coloros.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.coloros.helper.BatteryOptimizationHelper

/**
 * BatteryWhitelistActivity — 电池优化白名单引导页
 *
 * 通过分步引导用户完成 ColorOS 后台保活的所有设置
 */
class BatteryWhitelistActivity : ComponentActivity() {

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
                BatteryGuideScreen(
                    onOpenStandardWhitelist = { openIntent(getHelper().getWhitelistIntent()) },
                    onOpenAutoStart = { getHelper().getColorOsAutoStartIntent()?.let { openIntent(it) } },
                    onOpenBatteryDetail = { getHelper().getColorOsBatteryDetailIntent()?.let { openIntent(it) } },
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun getHelper(): BatteryOptimizationHelper {
        return BatteryOptimizationHelper(this)
    }

    private fun openIntent(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatteryGuideScreen(
    onOpenStandardWhitelist: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onOpenBatteryDetail: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val helper = remember { BatteryOptimizationHelper(context) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("电池优化设置", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 警告卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF3D1C00)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "⚠️ ColorOS 后台限制",
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ColorOS 默认会在锁屏 3 分钟后冻结后台 App。" +
                        "如果不完成以下设置，灵动岛将在后台停止工作。",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }

            // 引导文案
            Text(
                helper.getGuideText(),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(8.dp))

            // 快捷跳转按钮
            Text(
                "快捷跳转设置页",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onOpenStandardWhitelist,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1B5E20)
                )
            ) {
                Text("① 电池优化 → 不优化")
            }

            Button(
                onClick = onOpenAutoStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0D47A1)
                )
            ) {
                Text("② 自启动管理 → 开启")
            }

            Button(
                onClick = onOpenBatteryDetail,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A148C)
                )
            ) {
                Text("③ 电池详情 → 后台不冻结")
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成，返回")
            }
        }
    }
}
