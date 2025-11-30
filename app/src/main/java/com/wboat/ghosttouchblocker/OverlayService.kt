package com.wboat.ghosttouchblocker

import android.app.*
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

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val left = intent.getIntExtra("left", 0)
                val top = intent.getIntExtra("top", 0)
                val right = intent.getIntExtra("right", 0)
                val bottom = intent.getIntExtra("bottom", 0)
                val color = intent.getIntExtra("color", 0x30FF0000)
                val showFloating = intent.getBooleanExtra("showFloatingButton", false)
                startOverlay(left, top, right, bottom, color, showFloating)
            }
            ACTION_STOP -> stopOverlay()
        }
        return START_STICKY
    }

    private fun startOverlay(left: Int, top: Int, right: Int, bottom: Int, color: Int, showFloating: Boolean) {
        if (overlayView != null) return

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = View(this).apply {
            setBackgroundColor(color)
            setOnTouchListener { _, _ -> true }
        }

        val params = WindowManager.LayoutParams(
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

        windowManager?.addView(overlayView, params)

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
        overlayView?.let {
            windowManager?.removeView(it)
            overlayView = null
        }
        floatingButton?.let {
            windowManager?.removeView(it)
            floatingButton = null
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
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
            .build()
    }

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        private const val CHANNEL_ID = "overlay_service"
    }
}
