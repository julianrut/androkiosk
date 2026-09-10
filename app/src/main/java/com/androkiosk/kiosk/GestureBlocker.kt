package com.androkiosk.kiosk

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.core.content.getSystemService

private const val GESTURE_STRIP_DP = 24
private const val MIN_STATUS_BAR_DP = 28

class GestureBlocker(private val context: Context) {
    private val windowManager = context.getSystemService<WindowManager>()
    private var overlays = emptyList<View>()

    fun attach(statusBarInset: Int) {
        if (overlays.isNotEmpty()) return
        val manager = windowManager ?: return
        if (!KioskController.canBlockGestures(context)) return

        val added = mutableListOf<View>()
        listOf(
            Gravity.TOP to maxOf(statusBarInset, dp(MIN_STATUS_BAR_DP)),
            Gravity.BOTTOM to dp(GESTURE_STRIP_DP),
        ).forEach { (gravity, height) ->
            val overlay = View(context).apply { isClickable = true }
            runCatching { manager.addView(overlay, params(gravity, height)) }
                .onSuccess { added += overlay }
        }
        overlays = added
    }

    fun detach() {
        overlays.forEach { overlay -> runCatching { windowManager?.removeView(overlay) } }
        overlays = emptyList()
    }

    private fun params(gravity: Int, height: Int) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        height,
        overlayType(),
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).also { it.gravity = gravity }

    @Suppress("DEPRECATION")
    private fun overlayType() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR
    }

    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
}
