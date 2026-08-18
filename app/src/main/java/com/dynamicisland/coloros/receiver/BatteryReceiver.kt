package com.dynamicisland.coloros.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.dynamicisland.coloros.manager.IslandContentType
import com.dynamicisland.coloros.manager.IslandData
import com.dynamicisland.coloros.manager.IslandStateManager

/**
 * ============================================================
 * BatteryReceiver — 电池状态监听
 * ============================================================
 *
 * 功能：
 * 1. 监听充电/断电事件
 * 2. 实时更新电量百分比
 * 3. 低电量预警（≤20%）
 * 4. 计算预估充满时间（基于充电速率）
 *
 * 注册方式：Manifest 静态注册（已在 AndroidManifest.xml 声明）
 */
class BatteryReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BatteryReceiver"
        private const val LOW_BATTERY_THRESHOLD = 20
    }

    private lateinit var stateManager: IslandStateManager

    // 用于估算充满时间
    private var lastBatteryLevel = -1

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        stateManager = IslandStateManager.getInstance()

        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> handleBatteryChange(intent)
            Intent.ACTION_POWER_CONNECTED -> handlePowerConnected()
            Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
        }
    }

    /**
     * 电池状态变化（持续触发）
     */
    private fun handleBatteryChange(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

        if (level < 0 || scale <= 0) return

        val percentage = (level * 100 / scale.toFloat()).toInt()
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

        // 计算充电进度 0..1
        val progress = (percentage / 100f).coerceIn(0f, 1f)

        if (isCharging) {
            // 估算剩余充满时间
            val remainingMinutes = estimateRemainingTime(percentage, plugged)

            // 更新灵动岛充电信息
            stateManager.updateContent { data ->
                if (data.contentType == IslandContentType.CHARGING) {
                    data.copy(
                        subtitle = "约 ${remainingMinutes} 分钟充满",
                        progress = progress
                    )
                } else {
                    data
                }
            }

            // 如果当前不在充电态，推送新内容
            val current = stateManager.currentData.value
            if (current.contentType != IslandContentType.CHARGING) {
                stateManager.pushContent(
                    IslandData(
                        contentType = IslandContentType.CHARGING,
                        title = "充电中",
                        subtitle = "约 ${remainingMinutes} 分钟充满",
                        body = "",
                        progress = progress,
                        iconRes = android.R.drawable.ic_lock_idle_charging,
                        packageName = "system",
                        notificationId = 3001
                    )
                )
            }
        }

        // 低电量预警（仅当电量变化跨越阈值时触发）
        if (!isCharging && percentage <= LOW_BATTERY_THRESHOLD && percentage != lastBatteryLevel) {
            stateManager.pushContent(
                IslandData(
                    contentType = IslandContentType.NOTIFICATION,
                    title = "低电量提醒",
                    subtitle = "剩余 ${percentage}%",
                    body = "建议连接充电器",
                    iconRes = android.R.drawable.ic_dialog_alert,
                    packageName = "system",
                    notificationId = 3002
                )
            )
        }

        lastBatteryLevel = percentage
    }

    /**
     * 插入充电器
     */
    private fun handlePowerConnected() {
        stateManager.pushContent(
            IslandData(
                contentType = IslandContentType.CHARGING,
                title = "充电中",
                subtitle = "正在计算…",
                body = "",
                progress = 0f,
                iconRes = android.R.drawable.ic_lock_idle_charging,
                packageName = "system",
                notificationId = 3001
            )
        )
    }

    /**
     * 拔掉充电器
     */
    private fun handlePowerDisconnected() {
        val current = stateManager.currentData.value
        if (current.contentType == IslandContentType.CHARGING) {
            stateManager.dismissCurrent()
        }
    }

    /**
     * 估算剩余充满时间（分钟）
     *
     * 简化算法：假设充电速率约 1%/分钟（快充约 1.5%/分钟）
     * 实际项目可记录历史充电速率做更精确预测
     */
    private fun estimateRemainingTime(percentage: Int, plugged: Int): Int {
        if (percentage >= 100) return 0

        // 粗略判断快充（AC 通常为快充）
        val isFastCharge = plugged == BatteryManager.BATTERY_PLUGGED_AC
        val ratePerMinute = if (isFastCharge) 1.5f else 1.0f

        val remaining = (100 - percentage) / ratePerMinute
        return remaining.toInt().coerceAtLeast(1)
    }
}
