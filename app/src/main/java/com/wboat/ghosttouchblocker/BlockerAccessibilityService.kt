package com.wboat.ghosttouchblocker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.*
import android.view.accessibility.AccessibilityEvent

class BlockerAccessibilityService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var blockRegion: Rect? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val left = it.getIntExtra("left", 0)
            val top = it.getIntExtra("top", 0)
            val right = it.getIntExtra("right", 0)
            val bottom = it.getIntExtra("bottom", 0)
            blockRegion = Rect(left, top, right, bottom)
            showOverlay(left, top, right, bottom)
        }
        return START_STICKY
    }

    private fun showOverlay(left: Int, top: Int, right: Int, bottom: Int) {
        overlayView?.let { windowManager?.removeView(it) }

        overlayView = View(this).apply {
            setBackgroundColor(0x30FF0000)
            setOnTouchListener { _, _ -> true }
        }

        val params = WindowManager.LayoutParams(
            right - left,
            bottom - top,
            left,
            top,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager?.addView(overlayView, params)
    }

    fun stopBlocking() {
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        blockRegion = null
    }

    override fun onDestroy() {
        stopBlocking()
        super.onDestroy()
    }
}
