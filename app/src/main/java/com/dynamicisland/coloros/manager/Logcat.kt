package com.dynamicisland.coloros.manager

import android.util.Log

/**
 * 统一日志工具。
 *
 * 所有 Tag 以 "DI_" 前缀开头，方便在 Logcat 中过滤：
 *   过滤语法：tag:DI_*
 *
 * 调试时建议打开 VERBOSE，发布时可关闭。
 */
object Logcat {

    private const val TAG_PREFIX = "DI_"
    const val VERBOSE = true   // 发布前改为 false

    fun d(tag: String, msg: String) {
        if (VERBOSE) Log.d(TAG_PREFIX + tag, msg)
    }

    fun i(tag: String, msg: String) {
        Log.i(TAG_PREFIX + tag, msg)
    }

    fun w(tag: String, msg: String) {
        Log.w(TAG_PREFIX + tag, msg)
    }

    fun e(tag: String, msg: String) {
        Log.e(TAG_PREFIX + tag, msg)
    }
}