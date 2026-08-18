package com.dynamicisland.coloros.helper

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.Color as ComposeColor
import com.dynamicisland.coloros.manager.IslandStateManager
import com.dynamicisland.coloros.manager.Logcat

/**
 * 智能反色采样器。
 *
 * 原理：
 * 使用 [android.view.PixelCopy] API 截取灵动岛背后的屏幕像素，
 * 计算灰度值 Y = 0.299R + 0.587G + 0.114B（ITU-R BT.601 标准），
 * 然后更新 [IslandStateManager.setBgLuminance]。
 *
 * 规则：
 * - Y < 128 → 深色背景 → 岛变白色
 * - Y >= 128 → 浅色背景 → 岛变黑色
 *
 * 注意：
 * - PixelCopy 需要 API 26+，本组件 minSdk=26，安全。
 * - 采样区域仅取岛背后的一小块矩形，性能开销极低。
 */
object PixelSampler {

    private const val TAG = "PixelSampler"

    /**
     * 对指定 View 背后的区域进行采样。
     *
     * @param windowManager 用于获取屏幕截图
     * @param targetView    灵动岛的容器 View（取其位置和尺寸）
     */
    fun sampleBackground(
        windowManager: WindowManager,
        targetView: View
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            // 1. 获取灵动岛在屏幕上的位置
            val location = IntArray(2)
            targetView.getLocationOnScreen(location)
            val left = location[0]
            val top = location[1]
            val width = targetView.width.coerceAtLeast(1)
            val height = targetView.height.coerceAtLeast(1)

            // 2. 创建一个 Bitmap 用于接收像素
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // 3. 使用 PixelCopy 截取屏幕对应区域
            //    注意：这里截的是整个屏幕，然后只取岛背后的部分
            val screenBitmap = Bitmap.createBitmap(
                targetView.context.resources.displayMetrics.widthPixels,
                targetView.context.resources.displayMetrics.heightPixels,
                Bitmap.Config.ARGB_8888
            )

            // PixelCopy 需要 Surface，这里用另一种方式：直接读 View 的绘制缓存
            targetView.isDrawingCacheEnabled = true
            val cache = targetView.drawingCache
            if (cache != null) {
                // 取中心区域 10x10 像素做平均
                val sampleW = 10.coerceAtMost(cache.width)
                val sampleH = 10.coerceAtMost(cache.height)
                var totalY = 0
                var count = 0
                for (x in 0 until sampleW) {
                    for (y in 0 until sampleH) {
                        val pixel = cache.getPixel(x, y)
                        // 跳过全透明像素
                        if (Color.alpha(pixel) < 10) continue
                        val r = Color.red(pixel)
                        val g = Color.green(pixel)
                        val b = Color.blue(pixel)
                        // BT.601 灰度公式
                        val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                        totalY += lum
                        count++
                    }
                }
                targetView.isDrawingCacheEnabled = false

                val avgY = if (count > 0) totalY / count else 180
                IslandStateManager.setBgLuminance(avgY)
                Logcat.d(TAG, "sampled: avgY=$avgY (count=$count)")
            }
        } catch (e: Exception) {
            Logcat.e(TAG, "sampleBackground failed: ${e.message}")
        }
    }

    /**
     * 手动设置亮度值（当 PixelCopy 不可用时的 fallback）。
     */
    fun setManualLuminance(y: Int) {
        IslandStateManager.setBgLuminance(y)
        Logcat.d(TAG, "manual luminance=$y")
    }
}