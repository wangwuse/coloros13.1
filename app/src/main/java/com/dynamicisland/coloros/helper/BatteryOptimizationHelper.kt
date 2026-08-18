package com.dynamicisland.coloros.helper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * ============================================================
 * BatteryOptimizationHelper — 电池优化白名单引导
 * ============================================================
 *
 * ⚠️ ColorOS 13.1 专项适配（关键！）
 *
 * ColorOS 对后台限制极其严格：
 * 1. 默认将所有非白名单 App 加入"智能省电"
 * 2. 锁屏 3-5 分钟后冻结后台服务
 * 3. 即使 Android 标准电池白名单也不完全生效
 *
 * 解决方案（多管齐下）：
 * ✅ 引导用户加入 Android 标准电池白名单
 * ✅ 引导用户到 ColorOS 自带的"自启动管理"开启
 * ✅ 引导用户到"后台冻结"设置中排除本应用
 * ✅ 引导用户关闭"省电模式"
 *
 * 文档参考：
 * - OPPO 官方：https://open.oppomobile.com/
 * - ColorOS 后台管理：设置 → 电池 → 更多电池设置
 */
class BatteryOptimizationHelper(private val context: Context) {

    companion object {
        private const val TAG = "BatteryOptHelper"

        // OPPO / OnePlus 自启动管理页面
        // 不同 ColorOS 版本 intent action 可能不同
        private const val OPPO_AUTO_START = "com.coloros.safecenter.permission.startup.StartupAppListActivity"
        private const val OPPO_AUTO_START_ALT = "com.coloros.safecenter.startupapp.StartupAppListActivity"

        // 电池使用详情页
        private const val OPPO_BATTERY_DETAIL = "com.oplus.battery.ui.BatteryDetailActivity"
        private const val COLOROS_BATTERY_DETAIL = "com.coloros.safecenter.battery.BatteryDetailActivity"
    }

    /**
     * 检查是否已加入电池优化白名单
     */
    fun isBatteryWhitelisted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    /**
     * 获取引导用户加白名单的 Intent
     *
     * 优先使用 Android 标准方式，失败时尝试 ColorOS 私有页面
     */
    fun getWhitelistIntent(): Intent {
        // 方式1：Android 标准电池优化设置页
        val standard = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 检查是否有 Activity 能处理
        val resolveInfo = context.packageManager.resolveActivity(standard, 0)
        if (resolveInfo != null) {
            Log.d(TAG, "Using standard battery optimization settings")
            return standard
        }

        // 方式2：跳转到应用详情页（兜底）
        Log.d(TAG, "Falling back to application details settings")
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * 获取 ColorOS 自启动管理 Intent
     *
     * ColorOS 的"自启动管理"控制 App 是否能在后台自动启动
     * 这是 ColorOS 独有的限制，必须单独处理
     */
    fun getColorOsAutoStartIntent(): Intent? {
        // 尝试方案1
        val intent1 = Intent().apply {
            component = android.content.ComponentName(
                "com.coloros.safecenter",
                OPPO_AUTO_START
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (context.packageManager.resolveActivity(intent1, 0) != null) {
            return intent1
        }

        // 尝试方案2（旧版路径）
        val intent2 = Intent().apply {
            component = android.content.ComponentName(
                "com.coloros.safecenter",
                OPPO_AUTO_START_ALT
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (context.packageManager.resolveActivity(intent2, 0) != null) {
            return intent2
        }

        // 尝试方案3：通用安全中心
        val intent3 = Intent().apply {
            component = android.content.ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.MainActivity"
            )
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (context.packageManager.resolveActivity(intent3, 0) != null) {
            return intent3
        }

        // 全部失败 → 返回 null，调用方跳转到应用详情
        return null
    }

    /**
     * 获取 ColorOS 电池详情页 Intent
     */
    fun getColorOsBatteryDetailIntent(): Intent? {
        val intents = listOf(
            Intent().apply {
                component = android.content.ComponentName(
                    "com.oplus.battery",
                    OPPO_BATTERY_DETAIL
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            },
            Intent().apply {
                component = android.content.ComponentName(
                    "com.coloros.safecenter",
                    COLOROS_BATTERY_DETAIL
                )
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )

        for (intent in intents) {
            if (context.packageManager.resolveActivity(intent, 0) != null) {
                return intent
            }
        }
        return null
    }

    /**
     * 一键引导：依次跳转所有必要设置页
     *
     * 使用说明：
     * 1. 先检查 isBatteryWhitelisted()
     * 2. 如果未加入 → 调用 getWhitelistIntent() 跳转
     * 3. 然后调用 getColorOsAutoStartIntent() 引导自启动
     * 4. 最后调用 getColorOsBatteryDetailIntent() 设置后台不冻结
     */
    fun getAllRequiredIntents(): List<Intent> {
        val intents = mutableListOf<Intent>()

        // 1. 电池优化白名单
        if (!isBatteryWhitelisted()) {
            intents.add(getWhitelistIntent())
        }

        // 2. ColorOS 自启动管理
        getColorOsAutoStartIntent()?.let { intents.add(it) }

        // 3. ColorOS 电池详情
        getColorOsBatteryDetailIntent()?.let { intents.add(it) }

        return intents
    }

    /**
     * 生成用户引导文案（中文，直接展示在 UI 上）
     */
    fun getGuideText(): String {
        return """
            🔋 为保证灵动岛在后台持续运行，请完成以下设置：

            ① 电池优化 → 选择"不优化"
               （设置 → 电池 → 更多电池设置 → 找到本应用 → 不优化）

            ② 自启动管理 → 开启
               （设置 → 权限与隐私 → 自启动管理 → 开启本应用）

            ③ 后台冻结 → 排除本应用
               （设置 → 电池 → 省电模式 → 高级设置 → 排除应用）

            ④ 通知使用权 → 开启
               （设置 → 通知与状态栏 → 通知使用权 → 开启本应用）

            ⑤ 悬浮窗权限 → 开启
               （设置 → 权限与隐私 → 悬浮窗管理 → 开启本应用）

            ⚠️ ColorOS 默认会冻结后台 3 分钟以上的 App，
            如果不完成以上设置，灵动岛将在锁屏后停止工作。
        """.trimIndent()
    }
}
