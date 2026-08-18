package com.dynamicisland.coloros.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.model.IslandContent
import com.dynamicisland.coloros.model.IslandMode

/**
 * 充电状态监听接收器。
 *
 * 监听：
 * - ACTION_POWER_CONNECTED：插入充电器 → 切换到 LongPill 充电态
 * - ACTION_POWER_DISCONNECTED：拔掉充电器 → 回到 Compact
 * - BATTERY_CHANGED：电量变化 → 更新百分比
 *
 * 充电时灵动岛变为长条形态，显示电量和充电速度。
 */
class BatteryStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DI_Battery"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Logcat.d(TAG, "onReceive: $action")

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                IslandStateManager.setCharging(true)
                val level = getBatteryLevel(intent)
                IslandStateManager.setBatteryLevel(level)
                IslandStateManager.setContent(
                    IslandContent.Charging(level = level, isFastCharge = true)
                )
                IslandStateManager.setMode(IslandMode.LongPill)
                Logcat.i(TAG, "charging connected: $level%")
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                IslandStateManager.setCharging(false)
                IslandStateManager.setMode(IslandMode.Compact)
                IslandStateManager.setContent(IslandContent.Idle)
                Logcat.i(TAG, "charging disconnected")
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                val level = getBatteryLevel(intent)
                IslandStateManager.setBatteryLevel(level)
                val current = IslandStateManager.islandContent.value
                if (current is IslandContent.Charging) {
                    IslandStateManager.setContent(current.copy(level = level))
                }
                // 低电量预警（< 15%）
                if (level < 15 && !IslandStateManager.isCharging.value) {
                    IslandStateManager.setContent(
                        IslandContent.Charging(level = level, isFastCharge = false)
                    )
                    IslandStateManager.setMode(IslandMode.Compact)
                }
                Logcat.d(TAG, "battery level: $level%")
            }
        }
    }

    private fun getBatteryLevel(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) {
            (level * 100 / scale.toFloat()).toInt()
        } else 100
    }
}