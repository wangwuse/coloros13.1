package com.dynamicisland.coloros.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import com.dynamicisland.coloros.manager.IslandContentType
import com.dynamicisland.coloros.manager.IslandData
import com.dynamicisland.coloros.manager.IslandStateManager

/**
 * ============================================================
 * MusicControllerService — 音乐播放监听与控制
 * ============================================================
 *
 * 原理：
 * - 通过 MediaSessionManager 获取当前活跃的 MediaSession
 * - 注册 MediaController.Callback 监听播放状态/元数据变化
 * - 将音乐信息推送到灵动岛展示
 * - 支持发送播放/暂停/上一首/下一首指令
 *
 * ColorOS 注意：
 * - 部分音乐 App 可能不标准实现 MediaSession
 * - 需要在电池白名单中否则后台会被冻结
 * - 使用前台服务类型 mediaPlayback 提高优先级
 */
class MusicControllerService : Service() {

    companion object {
        private const val TAG = "MusicController"
        private const val ACTION_PLAY_PAUSE = "action_play_pause"
        private const val ACTION_NEXT = "action_next"
        private const val ACTION_PREV = "action_prev"

        fun start(context: Context, packageName: String) {
            val intent = Intent(context, MusicControllerService::class.java).apply {
                putExtra("package", packageName)
            }
            context.startService(intent)
        }

        fun sendAction(context: Context, action: String) {
            val intent = Intent(context, MusicControllerService::class.java).apply {
                putExtra("action", action)
            }
            context.startService(intent)
        }
    }

    private lateinit var stateManager: IslandStateManager
    private var mediaController: MediaController? = null

    // ====== 播放状态回调 ======
    private val callback = object : MediaController.Callback() {

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            metadata ?: return
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "未知歌曲"
            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "未知歌手"
            val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).toFloat()

            // 获取当前播放位置
            val pos = mediaController?.playbackState?.position ?: 0L
            val progress = if (duration > 0) (pos.toFloat() / duration).coerceIn(0f, 1f) else 0f

            // 获取当前包名
            val pkg = mediaController?.packageName ?: ""

            // 更新灵动岛
            stateManager.updateContent { data ->
                if (data.contentType == IslandContentType.MUSIC && data.packageName == pkg) {
                    data.copy(
                        title = title,
                        subtitle = artist,
                        body = "播放中…",
                        progress = progress
                    )
                } else data
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            // 可在此处理播放/暂停图标切换
        }
    }

    override fun onCreate() {
        super.onCreate()
        stateManager = IslandStateManager.getInstance()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.getStringExtra("action")
        val packageName = intent?.getStringExtra("package")

        // 处理按钮指令
        if (action != null) {
            handleAction(action)
            return START_STICKY
        }

        // 绑定到音乐 App 的 MediaSession
        if (packageName != null) {
            attachToMediaSession(packageName)
        }

        return START_STICKY
    }

    /**
     * 处理播放控制指令
     */
    private fun handleAction(action: String) {
        val keyCode = when (action) {
            ACTION_PLAY_PAUSE -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            ACTION_NEXT -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
            ACTION_PREV -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> return
        }

        mediaController?.let { controller ->
            val keyEvent = android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode)
            controller.dispatchMediaButtonEvent(keyEvent)
        }
    }

    /**
     * 绑定到指定包名的 MediaSession
     */
    private fun attachToMediaSession(packageName: String) {
        val sessionManager = getSystemService(MediaSessionManager::class.java) ?: return

        try {
            @Suppress("DEPRECATION")
            val sessions = sessionManager.getActiveSessions(
                android.content.ComponentName(this, NotificationCaptureService::class.java)
            )

            for (session in sessions) {
                if (session.packageName == packageName) {
                    // 清理旧控制器
                    mediaController?.unregisterCallback(callback)

                    // 创建新控制器
                    mediaController = MediaController(this, session.sessionToken)
                    mediaController?.registerCallback(callback)

                    // 立即获取当前元数据
                    val metadata = mediaController?.metadata
                    if (metadata != null) {
                        val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
                        val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
                        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION).toFloat()
                        val pos = mediaController?.playbackState?.position ?: 0L
                        val progress = if (duration > 0) (pos / duration).coerceIn(0f, 1f) else 0f

                        stateManager.pushContent(
                            IslandData(
                                contentType = IslandContentType.MUSIC,
                                title = title,
                                subtitle = artist,
                                body = "播放中…",
                                progress = progress,
                                packageName = packageName,
                                notificationId = 2001
                            )
                        )
                    }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        mediaController?.unregisterCallback(callback)
        mediaController = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
