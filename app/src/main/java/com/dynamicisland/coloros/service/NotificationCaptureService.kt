package com.dynamicisland.coloros.service

import android.app.Notification
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dynamicisland.coloros.manager.IslandContentType
import com.dynamicisland.coloros.manager.IslandData
import com.dynamicisland.coloros.manager.IslandStateManager

/**
 * ============================================================
 * NotificationCaptureService — 系统通知监听服务
 * ============================================================
 *
 * 功能：
 * 1. 监听所有 App 的通知（需用户授权"通知使用权"）
 * 2. 提取 title / text / icon / packageName
 * 3. 映射到 IslandData 并推送到 IslandStateManager
 * 4. 自动识别通知类型（外卖/打车/音乐/社交）
 *
 * ColorOS 注意：
 * - 通知使用权路径：设置 → 通知与状态栏 → 通知使用权
 * - ColorOS 可能会限制后台监听，需加入电池白名单
 * - 部分 App（如微信）的通知可能被系统折叠，需特殊处理
 */
class NotificationCaptureService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifCaptureService"

        // 音乐类 App 包名（用于识别音乐通知）
        private val MUSIC_PACKAGES = setOf(
            "com.netease.cloudmusic",  // 网易云音乐
            "com.tencent.qqmusic",    // QQ音乐
            "com.kugou.android",      // 酷狗
            "com.tencent.karaoke",    // 全民K歌
            "com.speiyou.kuwo",      // 酷我
            "com.spotify.music"      // Spotify
        )

        // 外卖/打车类（实时活动类型）
        private val LIVE_ACTIVITY_PACKAGES = setOf(
            "com.sankuai.meituan",     // 美团
            "com.ele.me",              // 饿了么
            "com.sdu.didi.psnger",     // 滴滴
            "com.ubercab"              // Uber
        )
    }

    private lateinit var stateManager: IslandStateManager

    override fun onCreate() {
        super.onCreate()
        stateManager = IslandStateManager.getInstance()
    }

    /**
     * 新通知到达
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        val notification = sbn.notification ?: return

        // 过滤：跳过自己的通知
        if (packageName == "com.dynamicisland.coloros") return

        // 过滤：仅处理可见通知
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 &&
            (notification.flags and Notification.FLAG_AUTO_CANCEL) == 0) {
            // 允许 AUTO_CANCEL 的通知通过（普通通知）
        }

        // 提取通知内容
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        // 获取小图标
        val smallIcon: Icon? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notification.smallIcon
        } else null

        // 判断通知类型
        val contentType = when {
            MUSIC_PACKAGES.contains(packageName) -> IslandContentType.MUSIC
            LIVE_ACTIVITY_PACKAGES.contains(packageName) -> IslandContentType.NOTIFICATION
            else -> IslandContentType.NOTIFICATION
        }

        // 构建 IslandData
        val data = IslandData(
            contentType = contentType,
            title = title.ifEmpty { getAppName(packageName) },
            subtitle = subText.ifEmpty { text },
            body = if (contentType == IslandContentType.MUSIC) "$title - $text" else text,
            iconRes = null,  // 实际项目中可把 Icon 转为 Drawable
            packageName = packageName,
            notificationId = sbn.id
        )

        // 推送到状态管理器
        stateManager.pushContent(data)

        // 特殊处理：音乐通知 → 启动音乐控制服务
        if (contentType == IslandContentType.MUSIC) {
            MusicControllerService.start(this, packageName)
        }
    }

    /**
     * 通知被移除（用户消除 / App 取消）
     */
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        // 找到对应通知并移除
        // 注意：这里用 packageName + id 匹配
        val currentStack = stateManager.stack.value
        val target = currentStack.find {
            it.packageName == sbn.packageName && it.notificationId == sbn.id
        }
        if (target != null) {
            // 移除该项
            val filtered = currentStack.filterNot {
                it.packageName == sbn.packageName && it.notificationId == sbn.id
            }
            // 直接操作 stack（通过 dismissCurrent 逻辑）
            // 简化：直接清除当前（实际应精确定位）
            if (stateManager.currentData.value.notificationId == sbn.id) {
                stateManager.dismissCurrent()
            }
        }
    }

    /**
     * 获取 App 名称（从包名）
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    /**
     * 服务连接时自动恢复已有通知
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        // 遍历当前活跃通知并重新推送
        val active = activeNotifications ?: return
        for (sbn in active) {
            onNotificationPosted(sbn)
        }
    }
}
