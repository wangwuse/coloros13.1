package com.dynamicisland.coloros.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.service.DynamicIslandService

/**
 * 开机自启 + 解锁拉起接收器。
 *
 * 监听：
 * - BOOT_COMPLETED：开机后自动启动灵动岛
 * - USER_PRESENT：用户解锁屏幕后拉起（ColorOS 锁屏清理后恢复）
 * - QUICKBOOT_POWERON：OPPO 快速开机
 */
class BootAndUnlockReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DI_Boot"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Logcat.i(TAG, "onReceive: $action")

        val serviceIntent = Intent(context, DynamicIslandService::class.java).apply {
            putExtra("from_boot", true)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Logcat.i(TAG, "service started from $action")
        } catch (e: Exception) {
            Logcat.e(TAG, "startService failed: ${e.message}")
        }
    }
}