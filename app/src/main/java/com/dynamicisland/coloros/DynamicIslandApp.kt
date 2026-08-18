package com.dynamicisland.coloros

import android.app.Application
import android.util.Log
import com.dynamicisland.coloros.manager.IslandStateManager

/**
 * 自定义 Application
 * App 启动时初始化全局状态管理器
 */
class DynamicIslandApp : Application() {

    companion object {
        private const val TAG = "DynamicIslandApp"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Application onCreate - PID: ${android.os.Process.myPid()}")

        // 初始化全局状态管理器（单例）
        IslandStateManager.init(applicationContext)

        // 注册未捕获异常处理
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "💥 Uncaught exception on ${thread.name}: ${throwable.message}", throwable)
        }
    }
}
