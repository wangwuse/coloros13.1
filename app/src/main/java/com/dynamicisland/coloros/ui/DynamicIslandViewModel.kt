package com.dynamicisland.coloros.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dynamicisland.coloros.manager.IslandContentType
import com.dynamicisland.coloros.manager.IslandData
import com.dynamicisland.coloros.manager.IslandMorph
import com.dynamicisland.coloros.manager.IslandStateManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * DynamicIslandViewModel
 *
 * 职责：将 IslandStateManager 的 StateFlow 暴露给 Compose UI，
 *       并提供用户交互事件的处理方法。
 *
 * 为什么用 AndroidViewModel 而不是 ViewModel？
 * → 因为悬浮窗服务也在用 IslandStateManager，
 *   ViewModel 在这里充当 UI 层的「状态持有者 + 事件处理器」，
 *   方便在 MainActivity 和 Compose 预览中使用。
 */
class DynamicIslandViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val stateManager = IslandStateManager.getInstance()

    // ====== 暴露给 Compose 的 StateFlow ======
    val currentData: StateFlow<IslandData> =
        stateManager.currentData.stateIn(viewModelScope, SharingStarted.Eagerly, IslandData())

    val morph: StateFlow<IslandMorph> =
        stateManager.morph.stateIn(viewModelScope, SharingStarted.Eagerly, IslandMorph.COMPACT)

    val isVisible: StateFlow<Boolean> =
        stateManager.isVisible.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val stack: StateFlow<List<IslandData>> =
        stateManager.stack.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val stackIndex: StateFlow<Int> =
        stateManager.stackIndex.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    // ==================== 事件处理 ====================

    /** 胶囊被点击 → 展开/收起切换 */
    fun onCapsuleClick() {
        val currentMorph = morph.value
        val newMorph = when (currentMorph) {
            IslandMorph.COMPACT -> IslandMorph.EXPANDED
            IslandMorph.EXPANDED -> IslandMorph.COMPACT
            IslandMorph.MINIMAL -> IslandMorph.COMPACT
            IslandMorph.LONG_PILL -> IslandMorph.EXPANDED
        }
        stateManager.setMorph(newMorph)
    }

    /** 胶囊被长按 → 触觉反馈 + 强制展开 */
    fun onCapsuleLongPress() {
        stateManager.setMorph(IslandMorph.EXPANDED)
    }

    /** 左滑 → 上一条 */
    fun onSwipeLeft() {
        stateManager.prevInStack()
    }

    /** 右滑 → 下一条 */
    fun onSwipeRight() {
        stateManager.nextInStack()
    }

    /** 下滑 → 收起/消除 */
    fun onSwipeDown() {
        val data = currentData.value
        if (data.contentType == IslandContentType.IDLE) {
            // 空闲态下滑 → 隐藏胶囊
            // stateManager.setIdle()  // 保持小胶囊
        } else {
            // 有内容下滑 → 消除当前
            stateManager.dismissCurrent()
        }
    }

    /** 跳转来源 App */
    fun onJumpToSource() {
        val data = currentData.value
        if (data.packageName.isNotEmpty()) {
            try {
                val pm = getApplication<Application>().packageManager
                val intent = pm.getLaunchIntentForPackage(data.packageName)
                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                intent?.let {
                    getApplication<Application>().startActivity(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** 手动测试：推送一条模拟通知 */
    fun pushTestNotification() {
        stateManager.pushContent(
            IslandData(
                contentType = IslandContentType.NOTIFICATION,
                title = "微信",
                subtitle = "新消息",
                body = "你有一条新的微信消息，点击查看详情",
                iconRes = android.R.drawable.ic_dialog_email,
                packageName = "com.tencent.mm",
                notificationId = 1001
            )
        )
    }

    /** 手动测试：模拟音乐播放 */
    fun pushTestMusic() {
        stateManager.pushContent(
            IslandData(
                contentType = IslandContentType.MUSIC,
                title = "晴天",
                subtitle = "周杰伦",
                body = "播放中…",
                progress = 0.35f,
                iconRes = android.R.drawable.ic_media_play,
                packageName = "com.netease.cloudmusic",
                notificationId = 2001
            )
        )
    }

    /** 手动测试：模拟充电 */
    fun pushTestCharging() {
        stateManager.pushContent(
            IslandData(
                contentType = IslandContentType.CHARGING,
                title = "充电中",
                subtitle = "约 45 分钟充满",
                body = "",
                progress = 0.67f,
                iconRes = android.R.drawable.ic_lock_idle_charging,
                packageName = "system",
                notificationId = 3001
            )
        )
    }

    /** 手动测试：启动计时器 */
    fun pushTestTimer() {
        stateManager.pushContent(
            IslandData(
                contentType = IslandContentType.TIMER,
                title = "番茄钟",
                subtitle = "剩余 18:32",
                body = "",
                progress = 0.26f,
                iconRes = android.R.drawable.ic_notification_overlay,
                packageName = "com.dynamicisland.coloros",
                notificationId = 4001
            )
        )
    }

    /** 清除所有 */
    fun clearAll() {
        // 直接重置栈
        repeat(stateManager.stack.value.size) {
            stateManager.dismissCurrent()
        }
    }
}
