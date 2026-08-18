package com.dynamicisland.coloros

import android.app.Application
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.manager.Logcat

/**
 * Application 入口。
 *
 * 职责：
 * 1. 初始化全局单例 [IslandStateManager]，确保 Service 和 Activity 共享同一份状态。
 * 2. 提供 Logcat 统一开关，方便 Reno7 真机调试。
 */
class DynamicIslandApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Logcat.d("DynamicIslandApp", "onCreate — Application 启动")
        IslandStateManager.init(this)
    }
}