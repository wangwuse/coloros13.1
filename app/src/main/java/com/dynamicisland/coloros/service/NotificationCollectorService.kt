package com.dynamicisland.coloros.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.model.IslandContent
import com.dynamicisland.coloros.model.IslandMode

/**
 * 通知监听服务。
 *
 * 通过 [NotificationListenerService] 捕获系统通知，
 * 将外卖/打车/音乐/进度类通知转化为灵动岛内容。
 *
 * 需要在系统设置中授予"通知使用权"权限。
 */
class NotificationCollectorService : NotificationListenerService() {

    companion object {
        private const val TAG = "DI_NotifSvc"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notif = sbn.notification ?: return
        val extras = notif.extras

        val title = extras.getString(android.app.Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(android.app.Notification.EXTRA_TEXT) ?: ""
        val pkg = sbn.packageName ?: ""
        val category = notif.category ?: ""

        Logcat.d(TAG, "posted: pkg=$pkg title=$title cat=$category")

        // 1. 音乐类通知 → 交给 MusicControllerService 处理
        if (category == android.app.Notification.CATEGORY_TRANSPORT ||
            pkg.contains("music") || pkg.contains("player") ||
            pkg.contains("spotify") || pkg.contains("qqmusic") ||
            pkg.contains("kugou") || pkg.contains("netease")) {
            // 不在这里处理音乐，由 MediaController 监听
            return
        }

        // 2. 进度类通知（外卖/下载/打车）→ 展开态
        val progress = extras.getInt(android.app.Notification.EXTRA_PROGRESS, -1)
        if (progress >= 0) {
            IslandStateManager.setContent(
                IslandContent.Notification(title = title, text = "$text ($progress%)", packageName = pkg)
            )
            IslandStateManager.setMode(IslandMode.Expanded)
            return
        }

        // 3. 普通通知 → Compact 态
        if (title.isNotEmpty() || text.isNotEmpty()) {
            IslandStateManager.setContent(
                IslandContent.Notification(title = title, text = text, packageName = pkg)
            )
            IslandStateManager.setMode(IslandMode.Compact)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Logcat.d(TAG, "removed: ${sbn?.packageName}")
        // 所有通知清除后回到空闲态
        // 延迟判断：如果还有其他通知，不应回到空闲
        // 简化实现：直接回到 Compact 空闲
        IslandStateManager.setMode(IslandMode.Compact)
        IslandStateManager.setContent(IslandContent.Idle)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Logcat.i(TAG, "NotificationListener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Logcat.w(TAG, "NotificationListener disconnected")
    }
}