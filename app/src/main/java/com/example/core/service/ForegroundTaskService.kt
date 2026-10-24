package com.example.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.core.permission.DynamicPermissionHandler

/**
 * Android 14+ (API 34+) Compliant Foreground Execution Service.
 */
class ForegroundTaskService : Service() {
    companion object {
        private const val TAG = "ForegroundTaskService"
        private const val NOTIFICATION_ID = 9102
        private const val CHANNEL_ID = "local_agent_unattended_channel"
        const val WAKELOCK_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes maximum
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!DynamicPermissionHandler.isPostNotificationsGranted(this)) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted. Cannot promote to foreground safely.")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification("Executing autonomous workflow in background...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        wakeLock?.acquire(WAKELOCK_TIMEOUT_MS)
        Log.i(TAG, "ForegroundTaskService started with WakeLock active.")
        return START_STICKY
    }

    private fun setupWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocalAgent::ForegroundTaskWakeLock")
        wakeLock?.setReferenceCounted(false)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LocalAgent Foreground Execution",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifies user when autonomous agent workflows run."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Local Agent Autonomous Core")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        wakeLock = null
        Log.i(TAG, "ForegroundTaskService destroyed. WakeLock released.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
