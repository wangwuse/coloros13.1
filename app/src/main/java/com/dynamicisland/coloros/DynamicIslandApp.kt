package com.dynamicisland.coloros

import android.app.Application
import android.util.Log
import com.dynamicisland.coloros.service.DynamicIslandService

/**
 * Application 入口
 *
 * 职责：
 * 1. 初始化全局 ViewModelStore（通过 ServiceLocator 模式）
 * 2. 在 Application 级别持有 IslandStateManager 单例，
 *    确保悬浮窗服务、通知监听、主 Activity 共享同一状态源
 */
class DynamicIslandApp : Application() {

    companion object {
        private const val TAG = "DynamicIslandApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate — ColorOS 13.1 Dynamic Island initializing")

        // 初始化全局状态管理器（单例）
        // 所有组件（Service / Activity / Receiver）通过 IslandStateManager.getInstance() 访问
        IslandStateManager.init(applicationContext)
    }
}
