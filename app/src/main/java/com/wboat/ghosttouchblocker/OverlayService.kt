package com.wboat.ghosttouchblocker

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var floatingButton: ImageView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var currentLeft = 0
    private var currentTop = 0
    private var currentRight = 0
    private var currentBottom = 0
    private var currentColor = 0
    private var showFloatingButton = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentLeft = intent?.getIntExtra("left", 0) ?: 0
                currentTop = intent?.getIntExtra("top", 0) ?: 0
                currentRight = intent?.getIntExtra("right", 0) ?: 0
                currentBottom = intent?.getIntExtra("bottom", 0) ?: 0
                currentColor = intent?.getIntExtra("color", 0x30FF0000) ?: 0x30FF0000
                showFloatingButton = intent?.getBooleanExtra("showFloatingButton", false) ?: false
                startOverlay(currentLeft, currentTop, currentRight, currentBottom, currentColor, showFloatingButton)
            }
            ACTION_STOP -> stopOverlay()
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (overlayView != null) {
            updateOverlayPosition()
        }
    }

    private fun updateOverlayPosition() {
        overlayParams?.let {
            it.width = currentRight - currentLeft
            it.height = currentBottom - currentTop
            it.x = currentLeft
            it.y = currentTop
            windowManager?.updateViewLayout(overlayView, it)
        }
    }

    private fun startOverlay(left: Int, top: Int, right: Int, bottom: Int, color: Int, showFloating: Boolean) {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = View(this).apply {
            setBackgroundColor(color)
            setOnTouchListener { _, _ -> true }
        }

        overlayParams = WindowManager.LayoutParams(
            right - left,
            bottom - top,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            x = left
            y = top
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager?.addView(overlayView, overlayParams)

        if (showFloating) {
            showFloatingStopButton()
        }
    }

    private fun showFloatingStopButton() {
        floatingButton = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_delete)
            setBackgroundColor(Color.parseColor("#CCFF0000"))
            setPadding(30, 30, 30, 30)
            setOnClickListener { stopOverlay() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 100
        }

        windowManager?.addView(floatingButton, params)
    }

    private fun stopOverlay() {
        try {
            overlayView?.let {
                windowManager?.removeView(it)
                overlayView = null
            }
            floatingButton?.let {
                windowManager?.removeView(it)
                floatingButton = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Touch Blocker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val startIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_START
            putExtra("left", currentLeft)
            putExtra("top", currentTop)
            putExtra("right", currentRight)
            putExtra("bottom", currentBottom)
            putExtra("color", currentColor)
            putExtra("showFloatingButton", showFloatingButton)
        }
        val startPendingIntent = PendingIntent.getService(
            this, 1, startIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.blocking_active))
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete,
                getString(R.string.stop_blocking),
                stopPendingIntent
            )
            .addAction(
                android.R.drawable.ic_input_add,
                getString(R.string.start_blocking),
                startPendingIntent
            )
            .build()
    }

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val CHANNEL_ID = "overlay_service"
    }
}
