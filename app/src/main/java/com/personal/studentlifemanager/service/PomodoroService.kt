package com.personal.studentlifemanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.personal.studentlifemanager.R
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroPhase
import com.personal.studentlifemanager.data.model.PomodoroRecord
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class PomodoroService : Service() {

    companion object {
        val timeLeft = MutableStateFlow(0L)
        val isRunning = MutableStateFlow(false)
        val currentPhase = MutableStateFlow(PomodoroPhase.FOCUS)
        val currentSession = MutableStateFlow(1)
        val isFinished = MutableStateFlow(false)
        var currentTaskName = "Tự do"

        fun sendCommand(context: Context, action: String, config: PomodoroConfig? = null, taskName: String? = null) {
            val intent = Intent(context, PomodoroService::class.java).apply {
                this.action = action
                if (config != null) {
                    putExtra("focus", config.focusTime)
                    putExtra("short", config.shortBreak)
                    putExtra("long", config.longBreak)
                    putExtra("sessions", config.sessionsCount)
                    putExtra("autoStart", config.autoStart)
                }
                if (taskName != null) {
                    putExtra("taskName", taskName)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private var configFocus = 25
    private var configShort = 5
    private var configLong = 15
    private var configSessions = 4
    private var autoStart = true
    private var focusStartTime = 0L

    private val CHANNEL_ID = "pomodoro_foreground_channel"
    private val NOTIFICATION_ID = 1

    private var wasPlayingBeforeCall = false

    // 🔥 1. BỘ NHẬN DIỆN CUỘC GỌI TỪ SIM (Telephony)
    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING, TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        if (isRunning.value) { wasPlayingBeforeCall = true; pauseTimer() }
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (wasPlayingBeforeCall) { wasPlayingBeforeCall = false; startTimer() }
                    }
                }
            }
        }
    }

    private lateinit var audioManager: AudioManager

    // 🔥 2. BỘ NHẬN DIỆN ZALO/MESSENGER/YOUTUBE/TIKTOK (Audio Focus)
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Có app khác (Zalo, Messenger) cướp quyền phát tiếng để đổ chuông -> Tự Pause
                if (isRunning.value) {
                    wasPlayingBeforeCall = true
                    pauseTimer()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // App kia cúp máy, trả lại quyền âm thanh -> Tự Resume
                if (wasPlayingBeforeCall) {
                    wasPlayingBeforeCall = false
                    startTimer()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Đăng ký bắt sóng SIM
        registerReceiver(callReceiver, IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED))
        // Khởi tạo Audio Manager để bắt sóng Zalo/Messenger
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            "ACTION_START" -> {
                configFocus = intent.getIntExtra("focus", 25)
                configShort = intent.getIntExtra("short", 5)
                configLong = intent.getIntExtra("long", 15)
                configSessions = intent.getIntExtra("sessions", 4)
                autoStart = intent.getBooleanExtra("autoStart", true)
                currentTaskName = intent.getStringExtra("taskName") ?: "Tự do"

                timeLeft.value = configFocus * 60L
                currentPhase.value = PomodoroPhase.FOCUS
                currentSession.value = 1
                focusStartTime = System.currentTimeMillis()
                isFinished.value = false

                startForegroundService()
                startTimer()
            }
            "ACTION_PAUSE" -> pauseTimer()
            "ACTION_RESUME" -> startTimer()
            "ACTION_ABORT" -> {
                if (currentPhase.value == PomodoroPhase.FOCUS) saveSessionToDb(isSuccess = false)
                stopTimer()
            }
            "ACTION_STOP" -> stopTimer()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification(formatTime(timeLeft.value), "Đang ${currentPhase.value.title.lowercase()}: $currentTaskName")
        startForeground(NOTIFICATION_ID, notification)
    }

    @Suppress("DEPRECATION")
    private fun startTimer() {
        isRunning.value = true
        timerJob?.cancel()

        // 🔥 Xin quyền âm thanh để làm mồi nhử bắt Zalo/Messenger
        audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        timerJob = serviceScope.launch {
            while (isRunning.value && timeLeft.value > 0) {
                delay(1000)
                timeLeft.value -= 1
                updateNotification()
            }

            if (isRunning.value && timeLeft.value == 0L) {
                vibratePhone()
                handlePhaseEnd()
            }
        }
    }

    private fun handlePhaseEnd() {
        when (currentPhase.value) {
            PomodoroPhase.FOCUS -> {
                saveSessionToDb(isSuccess = true)

                if (currentSession.value < configSessions) {
                    currentPhase.value = PomodoroPhase.SHORT_BREAK
                    timeLeft.value = configShort * 60L
                } else {
                    currentPhase.value = PomodoroPhase.LONG_BREAK
                    timeLeft.value = configLong * 60L
                }
                playSfx(R.raw.japanese_school_bell)
                if (!autoStart) pauseTimer() else startTimer()
            }
            PomodoroPhase.SHORT_BREAK -> {
                currentSession.value += 1
                currentPhase.value = PomodoroPhase.FOCUS
                timeLeft.value = configFocus * 60L
                focusStartTime = System.currentTimeMillis()

                playSfx(R.raw.japanese_school_bell)
                if (!autoStart) pauseTimer() else startTimer()
            }
            PomodoroPhase.LONG_BREAK -> {
                isRunning.value = false
                MediaPlayer.create(this, R.raw.ending_effect)?.start()
                stopTimer()
                isFinished.value = true
            }
        }
    }

    // LƯU TRỰC TIẾP LÊN FIREBASE FIRESTORE
    private fun saveSessionToDb(isSuccess: Boolean) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val endTime = System.currentTimeMillis()
        val actualMinutes = ((endTime - focusStartTime) / 60000).toInt()

        val record = PomodoroRecord(
            // ... (Giữ nguyên các tham số cũ của bạn)
            startTime = focusStartTime,
            endTime = endTime,
            configFocus = configFocus,
            configShort = configShort,
            configSessions = configSessions,
            configLong = configLong,
            isCompleted = isSuccess,
            actualFocusMinutes = if (isSuccess) configFocus else actualMinutes,
            taskName = currentTaskName
        )

        val ref = db.collection("users").document(userId).collection("pomodoro_records").document()
        record.id = ref.id
        ref.set(record).addOnSuccessListener {
            // 🔥 CHIẾN DỊCH 5: NẾU THÀNH CÔNG, TỰ ĐỘNG SYNC SANG HABIT MODULE
            if (isSuccess && currentTaskName.isNotBlank() && currentTaskName != "Tự do") {
                syncHabitProgress(userId, db, currentTaskName)
            }
        }
    }

    // 🔥 HÀM TỰ ĐỘNG ĐÁNH DẤU HOÀN THÀNH HABIT (Chạy ngầm không cần mở app)
    private fun syncHabitProgress(userId: String, db: FirebaseFirestore, habitName: String) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val habitsRef = db.collection("users").document(userId).collection("habits")

        // Tìm xem có Habit nào trùng tên với Task của Pomodoro không
        habitsRef.whereEqualTo("name", habitName).get().addOnSuccessListener { snapshot ->
            if (!snapshot.isEmpty) {
                val habitDoc = snapshot.documents[0]
                val habitId = habitDoc.id

                // Cập nhật lịch sử của Habit đó (Tick Done cho ngày hôm nay)
                val updateData = mapOf(
                    "history.$todayStr" to true, // Đánh dấu ngày hôm nay là True (Đã làm)
                    "lastCompleted" to System.currentTimeMillis()
                )
                habitsRef.document(habitId).update(updateData)
            }
        }
    }

    private fun pauseTimer() {
        isRunning.value = false
        timerJob?.cancel()
        updateNotification(formatTime(timeLeft.value), "Đang tạm dừng: $currentTaskName")
    }

    @Suppress("DEPRECATION")
    private fun stopTimer() {
        isRunning.value = false
        timerJob?.cancel()
        timeLeft.value = 0L

        // Trả lại quyền âm thanh
        audioManager.abandonAudioFocus(audioFocusChangeListener)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun updateNotification(timeStr: String = formatTime(timeLeft.value), content: String = "Đang ${currentPhase.value.title.lowercase()}: $currentTaskName") {
        val notification = createNotification(timeStr, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(timeStr: String, contentText: String): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(timeStr)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(isRunning.value)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Đồng hồ Pomodoro", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun formatTime(seconds: Long): String {
        return String.format("%02d:%02d", seconds / 60, seconds % 60)
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else { vibrator.vibrate(500) }
    }

    private fun playSfx(resId: Int) {
        MediaPlayer.create(this, resId)?.apply { setOnCompletionListener { release() }; start() }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        unregisterReceiver(callReceiver)
        audioManager.abandonAudioFocus(audioFocusChangeListener)
    }
}