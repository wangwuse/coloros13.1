package com.dynamicisland.coloros.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dynamicisland.coloros.R
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.model.IslandContent
import com.dynamicisland.coloros.model.IslandMode

/**
 * 音乐控制服务。
 *
 * 策略：
 * 1. 即使不播放音乐，也维持一个空的 MediaSession，
 *    ColorOS 对"正在播放媒体"的 App 限制最宽松。
 * 2. 通过 [MediaSessionManager] 监听系统活跃媒体会话。
 * 3. 将音乐信息推送到 [IslandStateManager]，驱动灵动岛 UI。
 *
 * 同时以 foregroundServiceType="mediaPlayback" 声明，
 * 利用系统对媒体服务的宽容度来保活。
 */
class MusicControllerService : Service() {

    companion object {
        private const val TAG = "DI_Music"
        private const val NOTIF_ID = 0x8765
        private const val CHANNEL_ID = "music_playback"
    }

    private var mediaSessionManager: MediaSessionManager? = null
    private val activeControllers = mutableListOf<MediaController>()
    private val controllerCallbacks = mutableListOf<MediaController.Callback>()

    // 空 MediaSession（保活用）
    private var dummySession: android.media.session.MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Logcat.d(TAG, "onCreate")

        // 1. 创建空 MediaSession 维持"正在播放"状态
        dummySession = android.media.session.MediaSession(this, "DynamicIslandDummy").apply {
            isActive = true
            setMetadata(
                MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, "")
                    .build()
            )
        }

        // 2. 注册前台服务通知
        startForeground(NOTIF_ID, buildNotification())

        // 3. 监听系统活跃媒体会话
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        registerActiveSessionListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logcat.d(TAG, "onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        Logcat.w(TAG, "onDestroy")
        // 注销所有回调
        for (i in activeControllers.indices) {
            try {
                activeControllers[i].unregisterCallback(controllerCallbacks[i])
            } catch (_: Exception) { }
        }
        activeControllers.clear()
        controllerCallbacks.clear()

        dummySession?.release()
        dummySession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ──────────────────────────────
    //  媒体会话监听
    // ──────────────────────────────

    private fun registerActiveSessionListener() {
        val sessions = mediaSessionManager?.activeSessions ?: return
        for (session in sessions) {
            addController(session)
        }
    }

    private fun addController(session: MediaController) {
        val callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                metadata ?: return
                val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                if (title.isEmpty()) return

                Logcat.d(TAG, "music: $title - $artist")
                IslandStateManager.setContent(
                    IslandContent.Music(title = title, artist = artist, isPlaying = true)
                )
                IslandStateManager.setMode(IslandMode.Compact)
            }

            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                state ?: return
                val playing = state.state == android.media.session.PlaybackState.STATE_PLAYING
                val current = IslandStateManager.islandContent.value
                if (current is IslandContent.Music) {
                    IslandStateManager.setContent(
                        current.copy(isPlaying = playing)
                    )
                }
            }
        }
        session.registerCallback(callback, android.os.Handler(mainLooper))
        activeControllers.add(session)
        controllerCallbacks.add(callback)
    }

    // ──────────────────────────────
    //  通知构建
    // ──────────────────────────────

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, com.dynamicisland.coloros.ui.MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("灵动岛音乐监听")
            .setContentText("保持媒体会话活跃")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}