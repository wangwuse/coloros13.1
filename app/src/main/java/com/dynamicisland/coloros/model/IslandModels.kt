package com.dynamicisland.coloros.model

/**
 * 灵动岛三种核心形态 + 充电专用长条形态。
 *
 * - Compact：默认药丸态，紧贴左上角盲孔。
 * - Expanded：点击后向下展开的卡片，展示详细信息。
 * - Minimal：极简态，仅显示一个小圆点（无内容时）。
 * - LongPill：充电/导航等场景的长条形态。
 */
enum class IslandMode {
    Minimal,
    Compact,
    Expanded,
    LongPill
}

/**
 * 灵动岛当前展示的内容类型。
 */
sealed class IslandContent {
    /** 空闲态 */
    object Idle : IslandContent()

    /** 音乐播放中 */
    data class Music(
        val title: String,
        val artist: String,
        val isPlaying: Boolean
    ) : IslandContent()

    /** 系统通知（外卖/打车/进度等） */
    data class Notification(
        val title: String,
        val text: String,
        val packageName: String
    ) : IslandContent()

    /** 充电中 */
    data class Charging(
        val level: Int,        // 0~100
        val isFastCharge: Boolean
    ) : IslandContent()

    /** 计时器/倒计时 */
    data class Timer(
        val remainingSec: Int
    ) : IslandContent()
}