package com.dynamicisland.coloros.receiver

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import com.dynamicisland.coloros.manager.Logcat
import com.dynamicisland.coloros.service.DynamicIslandService

/**
 * JobScheduler 复活接收器。
 *
 * 当主服务被 ColorOS 杀掉后，JobScheduler 会在约 15 分钟后触发此 JobService，
 * 重新拉起 [DynamicIslandService]。
 *
 * 这是保活链路的最后一道防线。
 */
class JobResurrectReceiver : JobService() {

    companion object {
        private const val TAG = "DI_Job"
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Logcat.w(TAG, "onStartJob — 尝试复活主服务！")

        val intent = Intent(this, DynamicIslandService::class.java).apply {
            putExtra("from_job", true)
        }

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Logcat.i(TAG, "resurrection: startService success")
        } catch (e: Exception) {
            Logcat.e(TAG, "resurrection failed: ${e.message}")
        }

        // 返回 false：任务完成，不需要后台继续运行
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Logcat.w(TAG, "onStopJob — Job 被系统停止")
        // 返回 true 表示希望重试
        return true
    }
}