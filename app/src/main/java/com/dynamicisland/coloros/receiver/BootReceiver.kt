package com.dynamicisland.coloros.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.dynamicisland.coloros.service.DynamicIslandService

/**
 * 开机自启动 + 解锁屏幕时拉起灵动岛服务
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "📡 Received: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "🔌 Boot completed, starting service...")
                DynamicIslandService.start(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                // 用户解锁后检查服务是否存活
                Log.d(TAG, "🔓 User unlocked, ensuring service is running...")
                DynamicIslandService.start(context)
            }
        }
    }
}
