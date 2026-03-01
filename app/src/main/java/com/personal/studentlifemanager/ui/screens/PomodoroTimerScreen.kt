package com.personal.studentlifemanager.ui.screens

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.MediaPlayer
import android.os.Build
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
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel(), // Gọi ViewModel
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentPhase by remember { mutableStateOf(PomodoroPhase.FOCUS) }
    var currentSession by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableLongStateOf(config.focusTime * 60L) }
    var isRunning by remember { mutableStateOf(true) }

    // 🔥 Biến quản lý trạng thái hiển thị Popup Dừng
    var showExitDialog by remember { mutableStateOf(false) }

    // 🔥 Ghi nhớ thời điểm bấm Bắt đầu
    val startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var bgmSelection by remember { mutableIntStateOf(0) }
    val bgmList = listOf(
        Pair("Tắt nhạc", 0),
        Pair("Mưa & Sấm chớp", R.raw.rain_thunderstorm),
        Pair("Sóng biển", R.raw.sea_wave),
        Pair("Rừng tuyết", R.raw.snow_falling_tree)
    )

    var bgmPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var sfxPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 1. Ẩn thanh điều hướng (Immersive Mode)
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.navigationBars())

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.navigationBars())
            bgmPlayer?.release()
            sfxPlayer?.release()
        }
    }

    // 2. Hệ thống phát nhạc (SFX & Nhạc nền)
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

    // 🔥 3. HÀM CHUYÊN DỤNG ĐỂ LƯU DATABASE
    fun saveSessionToDb(isSuccess: Boolean) {
        val endTime = System.currentTimeMillis()
        // Tính số phút thực tế đã cày được (Làm tròn xuống)
        val actualMinutes = ((endTime - startTime) / 60000).toInt()

        val record = PomodoroRecord(
            startTime = startTime,
            endTime = endTime,
            configFocus = config.focusTime,
            configShort = config.shortBreak,
            configSessions = config.sessionsCount,
            configLong = config.longBreak,
            isCompleted = isSuccess,
            // Nếu thành công thì tính full thời gian, nếu thất bại thì tính thời gian thực tế
            actualFocusMinutes = if (isSuccess) config.focusTime else actualMinutes
        )
        pomodoroViewModel.saveRecord(record)
    }

    // 🔥 4. BẮT SỰ KIỆN OUT APP (ĐẨY XUỐNG BACKGROUND)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (isRunning) {
                    isRunning = false
                    saveSessionToDb(isSuccess = false) // GHI NHẬN THẤT BẠI
                    sendAbortNotification(context)
                    onBack() // Ép văng ra ngoài
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 5. LOGIC ĐẾM NGƯỢC
    LaunchedEffect(isRunning, currentPhase) {
        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }

        if (isRunning && timeLeft == 0L) {
            when (currentPhase) {
                PomodoroPhase.FOCUS -> {
                    if (currentSession < config.sessionsCount) {
                        currentPhase = PomodoroPhase.SHORT_BREAK
                        timeLeft = config.shortBreak * 60L
                        playSfx(R.raw.japanese_school_bell)
                    } else {
                        // 🎉 HOÀN THÀNH TOÀN BỘ PHIÊN
                        isRunning = false
                        MediaPlayer.create(context.applicationContext, R.raw.ending_effect)?.start()

                        saveSessionToDb(isSuccess = true) // GHI NHẬN THÀNH CÔNG

                        onBack()
                    }
                }
                PomodoroPhase.SHORT_BREAK -> {
                    currentSession++
                    currentPhase = PomodoroPhase.FOCUS
                    timeLeft = config.focusTime * 60L
                    playSfx(R.raw.japanese_school_bell)
                }
                PomodoroPhase.LONG_BREAK -> { /* Tạm bỏ qua theo logic của bạn */ }
            }
        }
    }

    // --- GIAO DIỆN ---
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    val bgColor = when(currentPhase) {
        PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.background
        PomodoroPhase.SHORT_BREAK -> Color(0xFFE8F5E9)
        PomodoroPhase.LONG_BREAK -> Color(0xFFE3F2FD)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(bgColor).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        var expandedBgm by remember { mutableStateOf(false) }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 16.dp)) {
            FilledTonalButton(
                onClick = { expandedBgm = true },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFE0E0E0))
            ) {
                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(bgmList[bgmSelection].first)
            }
            DropdownMenu(expanded = expandedBgm, onDismissRequest = { expandedBgm = false }) {
                bgmList.forEachIndexed { index, pair ->
                    DropdownMenuItem(text = { Text(pair.first) }, onClick = { bgmSelection = index; expandedBgm = false })
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF1F8E9), modifier = Modifier.padding(bottom = 32.dp)) {
                Text("PHIÊN $currentSession / ${config.sessionsCount} - ${currentPhase.title}", fontWeight = FontWeight.Bold, color = Color(0xFF388E3C), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            Text(timeString, fontSize = 90.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)

            Spacer(modifier = Modifier.height(100.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                // 🔥 NÚT DỪNG (Kích hoạt Popup)
                IconButton(
                    onClick = {
                        isRunning = false // Tạm dừng đồng hồ trước khi hỏi
                        showExitDialog = true // Mở popup
                    },
                    modifier = Modifier.size(64.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Dừng", tint = Color(0xFFF44336), modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier.size(80.dp).background(Color(0xFF212121), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Tạm dừng", tint = Color.White, modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }

    // 🔥 POPUP XÁC NHẬN KHI BẤM NÚT STOP
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                // Bấm ra ngoài popup thì tiếp tục đếm ngược
                showExitDialog = false
                isRunning = true
            },
            title = { Text("Dừng Pomodoro?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn bỏ cuộc giữa chừng không? Lịch sử sẽ ghi nhận đây là một phiên thất bại.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        saveSessionToDb(isSuccess = false) // GHI NHẬN THẤT BẠI
                        onBack() // Văng ra ngoài
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)) // Nút màu đỏ cảnh báo
                ) {
                    Text("Chắc chắn dừng", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        isRunning = true // Tiếp tục chạy
                    }
                ) {
                    Text("Tiếp tục tập trung", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}
// 🔥 HÀM GỬI THÔNG BÁO KHI OUT APP BỊ THIẾU ĐÂY RỒI
fun sendAbortNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel("pomodoro_channel", "Cảnh báo Pomodoro", NotificationManager.IMPORTANCE_HIGH)
        manager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, "pomodoro_channel")
        .setSmallIcon(android.R.drawable.ic_dialog_alert) // Bạn có thể thay bằng icon app của bạn
        .setContentTitle("Phiên Pomodoro thất bại!")
        .setContentText("Bạn đã rời khỏi ứng dụng. Sự mất tập trung này đã khiến phiên bị hủy.")
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .build()

    manager.notify(2001, notification)
}