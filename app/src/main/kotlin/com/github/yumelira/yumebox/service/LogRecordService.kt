package com.github.yumelira.yumebox.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.R
import androidx.core.app.NotificationCompat
import com.github.yumelira.yumebox.clash.manager.ClashManager
import com.github.yumelira.yumebox.MainActivity
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.android.ext.android.inject
import timber.log.Timber

class LogRecordService : Service() {

    companion object {
        private const val TAG = "LogRecordService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "log_record_channel"
        private val CHANNEL_NAME = "日志记录"

        private const val ACTION_START = "com.github.yumelira.yumebox.LOG_START"
        private const val ACTION_STOP = "com.github.yumelira.yumebox.LOG_STOP"

        const val LOG_DIR = "logs"
        const val LOG_PREFIX = ""
        const val LOG_SUFFIX = ".log"

        @Volatile
        var isRecording: Boolean = false
            private set

        @Volatile
        var currentLogFileName: String? = null
            private set

        fun start(context: Context) {
            val intent = Intent(context, LogRecordService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LogRecordService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun getLogDir(context: Context): File {
            return File(context.filesDir, LOG_DIR).apply { mkdirs() }
        }

        private var instance: LogRecordService? = null
        fun writeLog(logLine: String) {
            instance?.writeLogInternal(logLine)
        }
    }

    private val clashManager: ClashManager by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var logWriter: BufferedWriter? = null
    private var logCollectJob: Job? = null
    private var writerJob: Job? = null
    private val logChannel = Channel<String>(Channel.UNLIMITED)
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        Timber.tag(TAG).d("日志记录服务创建")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("日志记录服务销毁")
        closeLogWriter()
        serviceScope.cancel()
        isRecording = false
        currentLogFileName = null
        instance = null
        super.onDestroy()
    }

    private fun writeLogInternal(logLine: String) {
        if (isRecording) {
            logChannel.trySend(logLine + "\n")
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun startRecording() {
        if (isRecording) {
            Timber.tag(TAG).d("日志记录已在进行中")
            return
        }

        try {
            val logDir = getLogDir(applicationContext)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "$LOG_PREFIX$timestamp$LOG_SUFFIX"
            logFile = File(logDir, fileName)
            logWriter = BufferedWriter(FileWriter(logFile, true))

            currentLogFileName = fileName
            isRecording = true

            startForeground(NOTIFICATION_ID, createNotification())

            // 启动写入协程
            writerJob = serviceScope.launch {
                while (isActive) {
                    // 等待第一条日志
                    val first = logChannel.receiveCatching().getOrNull() ?: break
                    try {
                        logWriter?.write(first)
                        // 进入批处理模式
                        var lastFlushTime = System.currentTimeMillis()
                        while (isActive) {
                            // 优先尝试直接获取，避免创建超时协程的开销
                            var next = logChannel.tryReceive().getOrNull()
                            if (next == null) {
                                // 如果没有立即获取到，则等待 1 秒
                                next = withTimeoutOrNull(1000) {
                                    logChannel.receiveCatching().getOrNull()
                                }
                            }
                            if (next != null) {
                                logWriter?.write(next)
                                // 如果距离上次刷新超过 5 秒，强制刷新以防数据丢失
                                if (System.currentTimeMillis() - lastFlushTime > 5000) {
                                    logWriter?.flush()
                                    lastFlushTime = System.currentTimeMillis()
                                }
                            } else {
                                // 1 秒内没有新日志，刷新并回到无限等待状态
                                logWriter?.flush()
                                break
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, "写入日志失败")
                    }
                }
            }
            logCollectJob = serviceScope.launch {
                Timber.tag(TAG).d("开始订阅日志")
                clashManager.logs.collect { log ->
                    if (isRecording) {
                        try {
                            val line = "[${dateFormat.format(log.time)}] [${log.level.name}] ${log.message}\n"
                            logChannel.send(line)
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "发送日志到通道失败")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动日志记录失败")
            isRecording = false
            currentLogFileName = null
            stopSelf()
        }
    }

    private fun stopRecording() {
        Timber.tag(TAG).d("停止日志记录")
        logCollectJob?.cancel()
        logCollectJob = null
        writerJob?.cancel()
        writerJob = null
        closeLogWriter()

        isRecording = false
        currentLogFileName = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeLogWriter() {
        try {
            logWriter?.flush()
            logWriter?.close()
            logWriter = null
            logFile = null
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "关闭日志写入器失败")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "日志记录服务通知"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LogRecordService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在记录日志")
            .setContentText(currentLogFileName ?: "日志记录中...")
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_menu_close_clear_cancel,
                "停止",
                stopPendingIntent
            )
            .build()
    }
}
