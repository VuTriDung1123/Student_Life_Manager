package com.personal.studentlifemanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.personal.studentlifemanager.data.model.PomodoroPhase // 🔥 THÊM DÒNG NÀY ĐỂ HẾT BÁO ĐỎ
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class PomodoroService : Service() {

    companion object {
        val timeLeft = MutableStateFlow(0L)
        val isRunning = MutableStateFlow(false)
        val currentPhase = MutableStateFlow(PomodoroPhase.FOCUS)
        val currentSession = MutableStateFlow(1)
        var currentTaskName = "Tự do"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val CHANNEL_ID = "pomodoro_foreground_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            "ACTION_START" -> {
                val focus = intent.getIntExtra("focus", 25)
                currentTaskName = intent.getStringExtra("taskName") ?: "Tự do"

                timeLeft.value = focus * 60L
                currentPhase.value = PomodoroPhase.FOCUS
                currentSession.value = 1

                startForegroundService()
                startTimer()
            }
            "ACTION_PAUSE" -> pauseTimer()
            "ACTION_RESUME" -> startTimer()
            "ACTION_STOP" -> stopTimer()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification(formatTime(timeLeft.value), "Đang tập trung: $currentTaskName")
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun startTimer() {
        isRunning.value = true
        timerJob?.cancel()

        timerJob = serviceScope.launch {
            while (isRunning.value && timeLeft.value > 0) {
                delay(1000)
                timeLeft.value -= 1
                updateNotification()
            }

            if (isRunning.value && timeLeft.value == 0L) {
                isRunning.value = false
                updateNotification("Hết giờ!", "Hoàn thành phiên")
            }
        }
    }

    private fun pauseTimer() {
        isRunning.value = false
        timerJob?.cancel()
        updateNotification(formatTime(timeLeft.value), "Đang tạm dừng: $currentTaskName")
    }

    private fun stopTimer() {
        isRunning.value = false
        timerJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun updateNotification(timeStr: String = formatTime(timeLeft.value), content: String = "Đang tập trung: $currentTaskName") {
        val notification = createNotification(timeStr, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(timeStr: String, contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(timeStr)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Đồng hồ Pomodoro", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format("%02d:%02d", m, s)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}