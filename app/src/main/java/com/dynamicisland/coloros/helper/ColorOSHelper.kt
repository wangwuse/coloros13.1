package com.dynamicisland.coloros.helper

import android.app.Activity
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.receiver.JobResurrectReceiver

/**
 * ColorOS / OPPO 专项保活工具类。
 *
 * ColorOS 13.1 的后台管理比原生 Android 严格 10 倍，普通 Service 锁屏 3 分钟必死。
 * 本类封装了所有必要的"求生"跳转和检测逻辑。
 *
 * 核心策略：
 * 1. 引导用户关闭电池优化（白名单）。
 * 2. 引导用户开启自启动 + 允许锁屏后台运行。
 * 3. 引导用户锁定后台（最近任务卡片下拉加锁）。
 * 4. 使用 JobScheduler 每 15 分钟尝试复活主服务。
 */
object ColorOSHelper {

    private const val TAG = "ColorOS"

    // ──────────────────────────────
    //  权限检测
    // ──────────────────────────────

    /**
     * 是否已加入电池优化白名单。
     */
    fun isBatteryWhitelisted(context: Context): Boolean {
        val pm = context.packageManager
        val name = context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val mode = Settings.System.getInt(
                context.contentResolver,
                "battery_optimization_whitelist", // ColorOS 自有字段
                0
            )
            // 标准 Android API
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            return powerManager.isIgnoringBatteryOptimizations(name)
        }
        return true
    }

    /**
     * 是否已授予悬浮窗权限。
     */
    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 是否已授予通知监听权限。
     */
    fun hasNotificationAccess(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(context.packageName)
    }

    // ──────────────────────────────
    //  跳转引导
    // ──────────────────────────────

    /**
     * 跳转电池优化白名单设置页。
     *
     * 优先尝试 ColorOS 自有页面（更友好），失败则 fallback 到标准 Android 页面。
     */
    fun requestBatteryWhitelist(activity: Activity, requestCode: Int = 1001) {
        // 方法1：ColorOS 专用 Intent
        val oppoIntent = Intent().apply {
            action = "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
            data = Uri.parse("package:${activity.packageName}")
        }
        try {
            activity.startActivityForResult(oppoIntent, requestCode)
            Logcat.d(TAG, "requestBatteryWhitelist: ColorOS path")
            return
        } catch (_: Exception) { }

        // 方法2：标准 Android API 23+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${activity.packageName}")
            }
            try {
                activity.startActivityForResult(intent, requestCode)
                Logcat.d(TAG, "requestBatteryWhitelist: standard path")
            } catch (e: Exception) {
                Logcat.e(TAG, "requestBatteryWhitelist failed: ${e.message}")
                // 终极 fallback：打开应用详情页，让用户手动找
                openAppDetails(activity)
            }
        }
    }

    /**
     * 跳转 ColorOS 自启动管理页面。
     *
     * OPPO/ColorOS 的自启动管理是独立的系统页面，不在标准 Android SDK 中。
     * 这里用已知可用的 Action 和 Component 尝试跳转。
     */
    fun requestAutoStart(activity: Activity) {
        val intents = arrayOf(
            // 方式1：ColorOS 自启动管理
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            // 方式2：OPPO 手机管家
            Intent().setComponent(
                ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            ),
            // 方式3：通用自启动
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            )
        )

        for (intent in intents) {
            try {
                activity.startActivity(intent)
                Logcat.d(TAG, "requestAutoStart success: ${intent.component}")
                return
            } catch (_: Exception) { }
        }
        Logcat.w(TAG, "requestAutoStart: all paths failed, fallback to app details")
        openAppDetails(activity)
    }

    /**
     * 跳转"允许后台活动/锁屏后台运行"设置。
     *
     * ColorOS 13.1 中路径：设置 → 电池 → 更多电池设置 → 允许后台活动。
     */
    fun requestBackgroundRun(activity: Activity) {
        val intents = arrayOf(
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.allowbackground.AllowBackgroundActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.privacypermissions.safecenter.PermissionActivity"
                )
            )
        )
        for (intent in intents) {
            try {
                activity.startActivity(intent)
                Logcat.d(TAG, "requestBackgroundRun success")
                return
            } catch (_: Exception) { }
        }
        // Fallback：电池设置页
        try {
            activity.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        } catch (_: Exception) {
            openAppDetails(activity)
        }
    }

    /**
     * 跳转悬浮窗权限设置页。
     */
    fun requestOverlayPermission(activity: Activity, requestCode: Int = 1002) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            try {
                activity.startActivityForResult(intent, requestCode)
                Logcat.d(TAG, "requestOverlayPermission")
            } catch (e: Exception) {
                Logcat.e(TAG, "requestOverlayPermission failed: ${e.message}")
            }
        }
    }

    /**
     * 跳转通知监听设置页。
     */
    fun requestNotificationAccess(activity: Activity, requestCode: Int = 1003) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        try {
            activity.startActivityForResult(intent, requestCode)
            Logcat.d(TAG, "requestNotificationAccess")
        } catch (e: Exception) {
            Logcat.e(TAG, "requestNotificationAccess failed: ${e.message}")
        }
    }

    /**
     * 打开应用详情页（终极 fallback）。
     */
    fun openAppDetails(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        activity.startActivity(intent)
    }

    // ──────────────────────────────
    //  JobScheduler 复活机制
    // ──────────────────────────────

    /**
     * 注册 JobScheduler，每 15 分钟尝试拉起主服务。
     *
     * ColorOS 对 JobScheduler 的限制相对宽松（系统核心服务依赖它），
     * 即使主进程被杀，JobScheduler 仍能在约 15 分钟后触发复活。
     */
    fun scheduleResurrection(context: Context) {
        val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val jobId = 0x1A1A  // 固定 ID，重复调度会覆盖

        val jobInfo = JobInfo.Builder(jobId, ComponentName(context, JobResurrectReceiver::class.java))
            .setPeriodic(15 * 60 * 1000L)       // 最小间隔 15 分钟
            .setPersisted(true)                   // 重启后依然生效
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
            .build()

        val result = js.schedule(jobInfo)
        if (result == JobScheduler.RESULT_SUCCESS) {
            Logcat.d(TAG, "JobScheduler: resurrection scheduled (15min)")
        } else {
            Logcat.w(TAG, "JobScheduler: schedule failed code=$result")
        }
    }

    /**
     * 取消复活任务（卸载/停止服务时调用）。
     */
    fun cancelResurrection(context: Context) {
        val js = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        js.cancel(0x1A1A)
        Logcat.d(TAG, "JobScheduler: resurrection cancelled")
    }
}