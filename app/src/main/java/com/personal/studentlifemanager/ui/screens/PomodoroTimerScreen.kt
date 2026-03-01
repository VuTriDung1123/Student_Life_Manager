package com.personal.studentlifemanager.ui.screens

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.personal.studentlifemanager.R
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroRecord
import kotlinx.coroutines.delay

enum class PomodoroPhase(val title: String) {
    FOCUS("TẬP TRUNG"),
    SHORT_BREAK("NGHỈ NGẮN"),
    LONG_BREAK("NGHỈ DÀI")
}

@Composable
fun PomodoroTimerScreen(
    config: PomodoroConfig,
    taskName: String,
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentPhase by remember { mutableStateOf(PomodoroPhase.FOCUS) }
    var currentSession by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableLongStateOf(config.focusTime * 60L) }
    var isRunning by remember { mutableStateOf(true) }

    var showExitDialog by remember { mutableStateOf(false) }

    // Ghi nhớ giờ bắt đầu của từng phiên Focus riêng biệt
    var focusStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var bgmSelection by remember { mutableIntStateOf(0) }
    val bgmList = listOf(Pair("Tắt nhạc", 0), Pair("Mưa & Sấm chớp", R.raw.rain_thunderstorm), Pair("Sóng biển", R.raw.sea_wave), Pair("Rừng tuyết", R.raw.snow_falling_tree))

    var bgmPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var sfxPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.navigationBars())
        onDispose { insetsController?.show(WindowInsetsCompat.Type.navigationBars()); bgmPlayer?.release(); sfxPlayer?.release() }
    }

    fun vibratePhone() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else { vibrator.vibrate(500) }
    }

    fun playSfx(resId: Int) {
        sfxPlayer?.release()
        sfxPlayer = MediaPlayer.create(context, resId)
        sfxPlayer?.start()
    }

    LaunchedEffect(bgmSelection, isRunning, currentPhase) {
        bgmPlayer?.release()
        bgmPlayer = null
        if (bgmSelection > 0 && isRunning && currentPhase == PomodoroPhase.FOCUS) {
            bgmPlayer = MediaPlayer.create(context, bgmList[bgmSelection].second)
            bgmPlayer?.isLooping = true
            bgmPlayer?.start()
        }
    }

    // LƯU DB: Chỉ lưu cho phiên Focus (Break không cần lưu)
    fun saveSessionToDb(isSuccess: Boolean) {
        val endTime = System.currentTimeMillis()
        val actualMinutes = ((endTime - focusStartTime) / 60000).toInt()

        val record = PomodoroRecord(
            startTime = focusStartTime,
            endTime = endTime,
            configFocus = config.focusTime,
            configShort = config.shortBreak,
            configSessions = config.sessionsCount,
            configLong = config.longBreak,
            isCompleted = isSuccess,
            actualFocusMinutes = if (isSuccess) config.focusTime else actualMinutes,
            taskName = taskName
        )
        pomodoroViewModel.saveRecord(record)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (isRunning) {
                    isRunning = false
                    if (currentPhase == PomodoroPhase.FOCUS) saveSessionToDb(isSuccess = false)
                    sendAbortNotification(context)
                    onBack()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // LOGIC CHUYỂN PHA POMODORO (ĐÃ SỬA LONG BREAK)
    LaunchedEffect(isRunning, currentPhase) {
        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }

        if (isRunning && timeLeft == 0L) {
            vibratePhone()
            when (currentPhase) {
                PomodoroPhase.FOCUS -> {
                    // Xong 1 phiên Focus -> Lưu Thành Công vào DB ngay!
                    saveSessionToDb(isSuccess = true)

                    if (currentSession < config.sessionsCount) {
                        currentPhase = PomodoroPhase.SHORT_BREAK
                        timeLeft = config.shortBreak * 60L
                    } else {
                        // Đủ N phiên -> CHUYỂN SANG LONG BREAK
                        currentPhase = PomodoroPhase.LONG_BREAK
                        timeLeft = config.longBreak * 60L
                    }
                    playSfx(R.raw.japanese_school_bell)
                    if (!config.autoStart) isRunning = false
                }
                PomodoroPhase.SHORT_BREAK -> {
                    currentSession++
                    currentPhase = PomodoroPhase.FOCUS
                    timeLeft = config.focusTime * 60L
                    focusStartTime = System.currentTimeMillis() // Reset giờ bắt đầu
                    playSfx(R.raw.japanese_school_bell)
                    if (!config.autoStart) isRunning = false
                }
                PomodoroPhase.LONG_BREAK -> {
                    // HẾT LONG BREAK -> HOÀN THÀNH TOÀN BỘ CHU TRÌNH
                    isRunning = false
                    MediaPlayer.create(context.applicationContext, R.raw.ending_effect)?.start()
                    onBack() // Văng ra ngoài, đã lưu dữ liệu ở Focus trước đó rồi
                }
            }
        }
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val bgColor = when(currentPhase) {
        PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.background
        PomodoroPhase.SHORT_BREAK -> Color(0xFFE8F5E9)
        PomodoroPhase.LONG_BREAK -> Color(0xFFE3F2FD)
    }

    Box(modifier = Modifier.fillMaxSize().background(bgColor).padding(24.dp), contentAlignment = Alignment.Center) {
        var expandedBgm by remember { mutableStateOf(false) }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp)) {
            FilledTonalButton(onClick = { expandedBgm = true }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE0E0E0))) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(bgmList[bgmSelection].first)
            }
            DropdownMenu(expanded = expandedBgm, onDismissRequest = { expandedBgm = false }) {
                bgmList.forEachIndexed { index, pair -> DropdownMenuItem(text = { Text(pair.first) }, onClick = { bgmSelection = index; expandedBgm = false }) }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F8E9), modifier = Modifier.padding(bottom = 16.dp)) {
                Text("PHIÊN $currentSession / ${config.sessionsCount} - ${currentPhase.title}", fontWeight = FontWeight.Bold, color = Color(0xFF388E3C), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Text(text = "Mục tiêu: $taskName", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.Gray, modifier = Modifier.padding(bottom = 16.dp))
            Text(timeString, fontSize = 90.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(100.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isRunning = false
                        showExitDialog = true
                    },
                    modifier = Modifier.size(64.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                ) { Icon(Icons.Default.Stop, contentDescription = "Dừng", tint = Color(0xFFF44336), modifier = Modifier.size(32.dp)) }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.size(80.dp).background(Color(0xFF212121), CircleShape)
                ) { Icon(imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Tạm dừng", tint = Color.White, modifier = Modifier.size(40.dp)) }
            }
        }
    }

    // 🔥 DIALOG KHI BẤM NÚT STOP (CÓ THÊM NÚT HỦY KHÔNG LƯU)
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false; isRunning = true },
            title = { Text("Dừng Pomodoro?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn muốn làm gì với phiên này?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        // Chỉ lưu Thất bại nếu đang ở pha Tập Trung
                        if (currentPhase == PomodoroPhase.FOCUS) saveSessionToDb(isSuccess = false)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Bỏ cuộc (Lưu thất bại)", color = Color.White, fontSize = 12.sp) }
            },
            dismissButton = {
                Row {
                    // 🔥 NÚT RESET: THOÁT RA MÀ KHÔNG LƯU GÌ CẢ
                    TextButton(onClick = { showExitDialog = false; onBack() }) {
                        Text("Hủy (Không lưu)", color = Color.Gray, fontSize = 12.sp)
                    }
                    TextButton(onClick = { showExitDialog = false; isRunning = true }) {
                        Text("Tiếp tục", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }
        )
    }
}

fun sendAbortNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("pomodoro_channel", "Cảnh báo Pomodoro", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }
    val notification = NotificationCompat.Builder(context, "pomodoro_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Phiên Pomodoro thất bại!")
        .setContentText("Bạn đã rời khỏi ứng dụng. Sự mất tập trung này đã khiến phiên bị hủy.")
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(2001, notification)
}