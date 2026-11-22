package com.example.focusflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class FocusService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private var tick: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannel(this)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "FocusFlow::Focus"
        ).apply { if (!isHeld) acquire() }

        val notification = buildNotification(this, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val runnable = object : Runnable {
            override fun run() {
                updateNotification(this@FocusService, FocusState.elapsedSeconds)
                handler.postDelayed(this, 1000)
            }
        }
        tick = runnable
        handler.post(runnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        tick?.let { handler.removeCallbacks(it) }
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Exception) {
            // 忽略 WakeLock 释放异常
        }
        wakeLock = null
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            // 忽略前台服务停止异常
        }
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "focusflow_channel"
        private const val NOTIFICATION_ID = 1001

        fun createChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "专注计时",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "专注模式运行状态" }
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(channel)
            }
        }

        fun updateNotification(context: Context, seconds: Long) {
            if (!FocusState.isFocusing) return
            try {
                createChannel(context)
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.notify(NOTIFICATION_ID, buildNotification(context, seconds))
            } catch (e: Exception) {
                // 模拟器上通知可能不可用，忽略即可
            }
        }

        private fun buildNotification(context: Context, seconds: Long): Notification {
            val stopIntent = Intent(context, StopReceiver::class.java)
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                2,
                openIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_focus_notification)
                .setContentTitle("专注中 · ${formatTime(seconds)}")
                .setContentText("屏幕常亮 · 点击返回专注界面")
                .setContentIntent(openPendingIntent)
                .addAction(R.drawable.ic_focus_notification, "结束专注", stopPendingIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        fun formatTime(totalSeconds: Long): String {
            val seconds = totalSeconds.coerceAtLeast(0)
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return "%02d:%02d:%02d".format(hours, minutes, secs)
        }
    }
}
