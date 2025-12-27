package com.github.yumelira.yumebox.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.R
import androidx.core.app.NotificationCompat
import com.github.yumelira.yumebox.MainActivity
import com.github.yumelira.yumebox.clash.manager.ClashManager
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class LogRecordService : Service() {

    companion object {
        private const val TAG = "LogRecordService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "log_record_channel"
        private val CHANNEL_NAME = MLang.Service.LogRecord.ChannelName

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
        Timber.tag(TAG).d(MLang.Service.LogRecord.Created)
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
        Timber.tag(TAG).d(MLang.Service.LogRecord.Destroyed)
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
            Timber.tag(TAG).d(MLang.Service.LogRecord.AlreadyRecording)
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

            Timber.tag(TAG).d(MLang.Service.LogRecord.RecordingStarted.format(fileName))

            // 启动写入协程
            writerJob = serviceScope.launch {
                for (line in logChannel) {
                    try {
                        logWriter?.write(line)
                        // 仅当通道为空时才刷新, 避免频繁IO
                        if (logChannel.isEmpty) {
                            logWriter?.flush()
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).e(e, MLang.Service.LogRecord.RecordingFailed)
                    }
                }
            }

            logCollectJob = serviceScope.launch {
                Timber.tag(TAG).d(MLang.Service.LogRecord.StartSubscribe)
                clashManager.logs.collect { log ->
                    if (isRecording) {
                        try {
                            val line = "[${dateFormat.format(log.time)}] [${log.level.name}] ${log.message}\n"
                            logChannel.send(line)
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, MLang.Service.LogRecord.SendToChannelFailed)
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, MLang.Service.LogRecord.StartFailed)
            isRecording = false
            currentLogFileName = null
            stopSelf()
        }
    }

    private fun stopRecording() {
        Timber.tag(TAG).d(MLang.Service.LogRecord.Stopped)
        
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
            Timber.tag(TAG).e(e, MLang.Service.LogRecord.StopFailed)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = MLang.Service.LogRecord.ChannelDescription
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
            .setContentTitle(MLang.Service.LogRecord.Recording)
            .setContentText(currentLogFileName ?: MLang.Service.LogRecord.RecordingFile)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_menu_close_clear_cancel,
                MLang.Service.LogRecord.ActionStop,
                stopPendingIntent
            )
            .build()
    }
}
