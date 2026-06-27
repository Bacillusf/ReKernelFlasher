package safe.kernel.flash

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class DaemonService : Service() {

    companion object {
        const val CHANNEL_ID = "flash_status"
        const val CHANNEL_NAME = "刷写状态"
        const val NOTIFICATION_ID = 1

        const val ACTION_START_FLASH = "safe.kernel.flash.START_FLASH"
        const val ACTION_FLASH_SUCCESS = "safe.kernel.flash.FLASH_SUCCESS"
        const val ACTION_FLASH_FAIL = "safe.kernel.flash.FLASH_FAIL"
        const val ACTION_STOP = "safe.kernel.flash.STOP"
        const val EXTRA_MESSAGE = "extra_message"

        var isRunning = false
            private set

        fun start(context: android.content.Context, message: String = "正在刷写...") {
            val intent = Intent(context, DaemonService::class.java).apply {
                action = ACTION_START_FLASH
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startForegroundService(intent)
        }

        fun notifySuccess(context: android.content.Context, message: String = "刷写成功") {
            val intent = Intent(context, DaemonService::class.java).apply {
                action = ACTION_FLASH_SUCCESS
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startService(intent)
        }

        fun notifyFail(context: android.content.Context, message: String = "刷写失败") {
            val intent = Intent(context, DaemonService::class.java).apply {
                action = ACTION_FLASH_FAIL
                putExtra(EXTRA_MESSAGE, message)
            }
            context.startService(intent)
        }

        fun stop(context: android.content.Context) {
            val intent = Intent(context, DaemonService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "刷写操作状态通知"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FLASH -> {
                val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: "正在刷写..."
                showNotification(msg, true)
            }
            ACTION_FLASH_SUCCESS -> {
                val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: "刷写成功"
                showNotification(msg, false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
            ACTION_FLASH_FAIL -> {
                val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: "刷写失败"
                showNotification(msg, false)
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun showNotification(message: String, ongoing: Boolean) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, Class.forName("safe.kernel.flash.MainActivity")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReKernelFlasher")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(ongoing)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (ongoing) {
            startForeground(NOTIFICATION_ID, notification)
        } else {
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }
}
