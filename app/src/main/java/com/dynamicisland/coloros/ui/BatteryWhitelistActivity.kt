package com.dynamicisland.coloros.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.coloros.helper.ColorOSHelper

/**
 * 电池白名单引导页。
 *
 * 首次启动时弹出，引导用户：
 * 1. 关闭电池优化
 * 2. 开启自启动
 * 3. 允许锁屏后台运行
 *
 * 这是保活链路的第一道关卡，必须让用户完成。
 */
class BatteryWhitelistActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WhitelistScreen(
                onBattery = { ColorOSHelper.requestBatteryWhitelist(this) },
                onAutoStart = { ColorOSHelper.requestAutoStart(this) },
                onBackground = { ColorOSHelper.requestBackgroundRun(this) },
                onDone = { finish() }
            )
        }
    }
}

@Composable
private fun WhitelistScreen(
    onBattery: () -> Unit,
    onAutoStart: () -> Unit,
    onBackground: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "保活设置引导",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "ColorOS 后台限制极严，需手动开启以下权限",
            color = Color.Gray,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                StepItem(
                    step = "①",
                    title = "关闭电池优化",
                    desc = "将本应用加入电池白名单，防止后台被杀",
                    onClick = onBattery
                )
                Spacer(Modifier.height(12.dp))
                StepItem(
                    step = "②",
                    title = "开启自启动",
                    desc = "允许开机和锁屏后自动拉起灵动岛",
                    onClick = onAutoStart
                )
                Spacer(Modifier.height(12.dp))
                StepItem(
                    step = "③",
                    title = "允许后台运行",
                    desc = "ColorOS 特有设置，锁屏后保持活动",
                    onClick = onBackground
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onDone,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("已完成，进入主界面", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StepItem(step: String, title: String, desc: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text("$step $title", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(2.dp))
            Text(desc, color = Color.Gray, fontSize = 11.sp)
        }
    }
}