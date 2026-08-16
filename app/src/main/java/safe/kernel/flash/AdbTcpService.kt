package safe.kernel.flash

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import com.topjohnwu.superuser.Shell

/**
 * Foreground service that keeps an ongoing notification while the wireless adbd (adb over TCP)
 * port is enabled, and handles the notification's "停止" (stop) action.
 */
class AdbTcpService : Service() {

    companion object {
        const val CHANNEL_ID = "adb_tcp"
        const val CHANNEL_NAME = "无线调试"
        const val NOTIFICATION_ID = 2

        private const val ACTION_START = "safe.kernel.flash.ADB_TCP_START"
        private const val ACTION_STOP = "safe.kernel.flash.ADB_TCP_STOP"
        private const val ACTION_DISMISS = "safe.kernel.flash.ADB_TCP_DISMISS"
        private const val EXTRA_PORT = "extra_port"

        val isRunning = mutableStateOf(false)
        val currentPort = mutableStateOf(0)

        fun start(context: Context, port: Int) {
            isRunning.value = true
            currentPort.value = port
            val intent = Intent(context, AdbTcpService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
            }
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AdbTcpService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun dismiss(context: Context) {
            isRunning.value = false
            currentPort.value = 0
            val intent = Intent(context, AdbTcpService::class.java).apply {
                action = ACTION_DISMISS
            }
            context.startService(intent)
        }

        fun syncDetected(context: Context, port: Int?) {
            if (port != null) {
                if (!isRunning.value || currentPort.value != port) {
                    start(context, port)
                }
            } else {
                if (isRunning.value) {
                    dismiss(context)
                }
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val port = intent.getIntExtra(EXTRA_PORT, 0)
                showNotification(port)
            }
            ACTION_STOP -> {
                runStopCommand()
            }
            ACTION_DISMISS -> {
                isRunning.value = false
                currentPort.value = 0
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun runStopCommand() {
        Thread {
            Shell.cmd("setprop service.adb.tcp.port 0 && stop adbd && start adbd").exec()
            mainHandler.post {
                isRunning.value = false
                currentPort.value = 0
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun showNotification(port: Int) {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, Class.forName("safe.kernel.flash.MainActivity")),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, AdbTcpService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ReKernelFlasher")
            .setContentText("adbd服务已经在${port}上开启，可以在电脑上使用adb connect命令连接")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", stopPendingIntent)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
