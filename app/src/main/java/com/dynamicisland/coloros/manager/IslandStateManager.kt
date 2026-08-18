package com.dynamicisland.coloros.manager

import android.content.Context
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 灵动岛当前展示的「内容类型」
 *
 * - IDLE：空闲，仅显示一个小胶囊（模拟挖孔）
 * - NOTIFICATION：系统通知（外卖/打车/社交等）
 * - MUSIC：音乐播放控制
 * - CHARGING：充电状态
 * - TIMER：倒计时/计时器
 * - NAVIGATION：导航提示（如"前方500米右转"）
 */
enum class IslandContentType {
    IDLE,
    NOTIFICATION,
    MUSIC,
    CHARGING,
    TIMER,
    NAVIGATION
}

/**
 * 灵动岛「形态」
 *
 * - COMPACT：小胶囊（约 120×40dp）
 * - EXPANDED：展开大卡片（向下展开，圆角 28dp）
 * - MINIMAL：极小图标（仅状态指示，如静音/导航箭头）
 * - LONG_PILL：长条状（充电进度条 / 计时器圆环）
 */
enum class IslandMorph {
    COMPACT,
    EXPANDED,
    MINIMAL,
    LONG_PILL
}

/**
 * 单条灵动岛数据（类似 iOS Live Activity 的 activityState）
 */
data class IslandData(
    val contentType: IslandContentType = IslandContentType.IDLE,
    val title: String = "",
    val subtitle: String = "",
    val body: String = "",
    val progress: Float = 0f,           // 0..1，用于充电/计时器进度
    val iconRes: Int? = null,           // 本地图标资源
    val iconDrawable: Drawable? = null, // 远程通知图标
    val packageName: String = "",       // 来源 App（用于点击跳转）
    val notificationId: Int = -1,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 全局状态管理器（单例 + StateFlow）
 *
 * 设计要点：
 * 1. 所有数据变更通过 updateState {} 进行，保证不可变 + 线程安全
 * 2. UI 层（Compose）通过 collectAsState() 自动响应变化
 * 3. 支持「堆叠」：最多保留 5 条历史，左右滑切换
 */
class IslandStateManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: IslandStateManager? = null

        fun init(context: Context) {
            if (INSTANCE == null) {
                synchronized(this) {
                    if (INSTANCE == null) {
                        INSTANCE = IslandStateManager(context.applicationContext)
                    }
                }
            }
        }

        fun getInstance(): IslandStateManager {
            return INSTANCE ?: throw IllegalStateException(
                "IslandStateManager not initialized! Call init() in Application.onCreate()"
            )
        }
    }

    // ====== 当前展示的内容 ======
    private val _currentData = MutableStateFlow(IslandData())
    val currentData: StateFlow<IslandData> = _currentData.asStateFlow()

    // ====== 当前形态（驱动动画）======
    private val _morph = MutableStateFlow(IslandMorph.COMPACT)
    val morph: StateFlow<IslandMorph> = _morph.asStateFlow()

    // ====== 是否可见（入场/退场动画）======
    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    // ====== 任务堆叠（最多5条）=====
    private val _stack = MutableStateFlow<List<IslandData>>(emptyList())
    val stack: StateFlow<List<IslandData>> = _stack.asStateFlow()

    // ====== 当前堆叠索引 ======
    private val _stackIndex = MutableStateFlow(0)
    val stackIndex: StateFlow<Int> = _stackIndex.asStateFlow()

    // ==================== 公开 API ====================

    /**
     * 推送一条新内容（如新通知到来）
     * 自动加入堆叠栈顶，并切换到该内容
     */
    fun pushContent(data: IslandData) {
        _stack.update { stack ->
            // 去重：同 packageName + notificationId 的更新
            val filtered = stack.filterNot {
                it.packageName == data.packageName &&
                it.notificationId == data.notificationId &&
                data.notificationId != -1
            }
            (filtered + data).takeLast(5) // 最多保留5条
        }
        _stackIndex.value = _stack.value.lastIndex.coerceAtLeast(0)
        _currentData.value = data
        _isVisible.value = true

        // 根据内容类型自动选择形态
        _morph.value = when (data.contentType) {
            IslandContentType.IDLE -> IslandMorph.COMPACT
            IslandContentType.MUSIC -> IslandMorph.EXPANDED
            IslandContentType.CHARGING -> IslandMorph.LONG_PILL
            IslandContentType.TIMER -> IslandMorph.LONG_PILL
            IslandContentType.NAVIGATION -> IslandMorph.COMPACT
            IslandContentType.NOTIFICATION -> IslandMorph.COMPACT
        }
    }

    /**
     * 更新当前内容（如音乐进度刷新）
     */
    fun updateContent(update: (IslandData) -> IslandData) {
        val updated = update(_currentData.value)
        _currentData.value = updated
        // 同步更新栈中对应项
        _stack.update { stack ->
            stack.map {
                if (it.packageName == updated.packageName &&
                    it.notificationId == updated.notificationId) updated else it
            }
        }
    }

    /**
     * 清除当前内容（如通知被用户消除）
     */
    fun dismissCurrent() {
        val currentStack = _stack.value.toMutableList()
        if (currentStack.isNotEmpty()) {
            currentStack.removeAt(_stackIndex.value)
            _stack.value = currentStack
        }
        if (currentStack.isEmpty()) {
            // 栈空 → 回到空闲
            _currentData.value = IslandData()
            _morph.value = IslandMorph.COMPACT
            _isVisible.value = false
        } else {
            // 显示栈顶
            _stackIndex.value = (currentStack.lastIndex).coerceAtLeast(0)
            _currentData.value = currentStack[_stackIndex.value]
        }
    }

    /**
     * 切换到下一条（右滑）
     */
    fun nextInStack() {
        val size = _stack.value.size
        if (size <= 1) return
        val newIndex = (_stackIndex.value + 1).coerceAtMost(size - 1)
        _stackIndex.value = newIndex
        _currentData.value = _stack.value[newIndex]
    }

    /**
     * 切换到上一条（左滑）
     */
    fun prevInStack() {
        val size = _stack.value.size
        if (size <= 1) return
        val newIndex = (_stackIndex.value - 1).coerceAtLeast(0)
        _stackIndex.value = newIndex
        _currentData.value = _stack.value[newIndex]
    }

    /**
     * 设置形态（点击展开/收起）
     */
    fun setMorph(morph: IslandMorph) {
        _morph.value = morph
    }

    /**
     * 回到空闲态
     */
    fun setIdle() {
        _currentData.value = IslandData()
        _morph.value = IslandMorph.COMPACT
        _isVisible.value = true // 仍显示小胶囊
    }
}
