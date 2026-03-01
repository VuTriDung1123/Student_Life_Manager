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
import kotlinx.coroutines.delay

enum class PomodoroPhase(val title: String) {
    FOCUS("TẬP TRUNG"),
    SHORT_BREAK("NGHỈ NGẮN"),
    LONG_BREAK("NGHỈ DÀI")
}

@Composable
fun PomodoroTimerScreen(
    config: PomodoroConfig,
    onBack: () -> Unit // Gọi khi bấm Stop hoặc khi Hoàn thành xong
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State quản lý thời gian và chu kỳ
    var currentPhase by remember { mutableStateOf(PomodoroPhase.FOCUS) }
    var currentSession by remember { mutableIntStateOf(1) }
    var timeLeft by remember { mutableLongStateOf(config.focusTime * 60L) }
    var isRunning by remember { mutableStateOf(true) }

    // State quản lý nhạc nền (0: Tắt, 1: Mưa, 2: Sóng biển, 3: Tuyết)
    var bgmSelection by remember { mutableIntStateOf(0) }
    val bgmList = listOf(
        Pair("Tắt nhạc", 0),
        Pair("Mưa & Sấm chớp", R.raw.rain_thunderstorm),
        Pair("Sóng biển", R.raw.sea_wave),
        Pair("Rừng tuyết", R.raw.snow_falling_tree)
    )

    // Khởi tạo MediaPlayer
    var bgmPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var sfxPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // 🔥 1. ẨN THANH ĐIỀU HƯỚNG BÊN DƯỚI (IMMERSIVE MODE)
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }

        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.navigationBars()) // Ẩn thanh Home

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.navigationBars()) // Hiện lại khi thoát
            bgmPlayer?.release()
            sfxPlayer?.release()
        }
    }

    // 🔥 2. HỆ THỐNG PHÁT NHẠC
    fun playSfx(resId: Int, onComplete: (() -> Unit)? = null) {
        sfxPlayer?.release()
        sfxPlayer = MediaPlayer.create(context, resId)
        if (onComplete != null) {
            sfxPlayer?.setOnCompletionListener {
                onComplete.invoke()
            }
        }
        sfxPlayer?.start()
    }

    LaunchedEffect(bgmSelection, isRunning, currentPhase) {
        bgmPlayer?.release()
        bgmPlayer = null
        // Chỉ phát nhạc nền khi đang chạy và ở trạng thái Tập Trung
        if (bgmSelection > 0 && isRunning && currentPhase == PomodoroPhase.FOCUS) {
            bgmPlayer = MediaPlayer.create(context, bgmList[bgmSelection].second)
            bgmPlayer?.isLooping = true
            bgmPlayer?.start()
        }
    }

    // 🔥 3. BẮT SỰ KIỆN OUT APP (ĐẨY XUỐNG BACKGROUND)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                if (isRunning) {
                    isRunning = false
                    sendAbortNotification(context)
                    onBack() // Ép văng ra ngoài màn hình chính
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 🔥 4. LOGIC ĐẾM NGƯỢC VÀ CHUYỂN PHA
    LaunchedEffect(isRunning, currentPhase) {
        while (isRunning && timeLeft > 0) {
            delay(1000)
            timeLeft--
        }

        // Khi hết giờ
        if (isRunning && timeLeft == 0L) {
            when (currentPhase) {
                PomodoroPhase.FOCUS -> {
                    if (currentSession < config.sessionsCount) {
                        // Chưa đủ phiên -> Cho nghỉ ngắn
                        currentPhase = PomodoroPhase.SHORT_BREAK
                        timeLeft = config.shortBreak * 60L
                        playSfx(R.raw.japanese_school_bell)
                    } else {
                        // 🎉 ĐÃ CÀY ĐỦ SỐ PHIÊN -> HOÀN THÀNH LUÔN!
                        isRunning = false

                        // 🔥 TÀ THUẬT: Dùng applicationContext để bài nhạc hát xuyên không gian, không bị tắt khi onBack()
                        MediaPlayer.create(context.applicationContext, R.raw.ending_effect)?.start()

                        // TODO: Gọi hàm lưu Data Hoàn Thành vào Database ở đây

                        onBack() // Văng ra ngoài luôn, không cần nghỉ!
                    }
                }
                PomodoroPhase.SHORT_BREAK -> {
                    // Nghỉ ngắn xong -> Quay lại cày phiên tiếp theo
                    currentSession++
                    currentPhase = PomodoroPhase.FOCUS
                    timeLeft = config.focusTime * 60L
                    playSfx(R.raw.japanese_school_bell)
                }
                PomodoroPhase.LONG_BREAK -> {
                    // Tạm ẩn pha này vì theo logic thực tế, cày xong N phiên là OUT luôn.
                }
            }
        }
    }

    // 🔥 GIAO DIỆN CHÍNH
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    // Đổi màu nền tùy theo trạng thái
    val bgColor = when(currentPhase) {
        PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.background
        PomodoroPhase.SHORT_BREAK -> Color(0xFFE8F5E9) // Xanh lá nhạt
        PomodoroPhase.LONG_BREAK -> Color(0xFFE3F2FD)  // Xanh dương nhạt
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Nút chọn nhạc nền ở góc trên phải
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
                    DropdownMenuItem(
                        text = { Text(pair.first) },
                        onClick = { bgmSelection = index; expandedBgm = false }
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Label Pha hiện tại
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF1F8E9),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "PHIÊN $currentSession / ${config.sessionsCount} - ${currentPhase.title}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Đồng hồ đếm ngược siêu to
            Text(
                text = timeString,
                fontSize = 90.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(100.dp))

            // Các nút điều khiển
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nút Dừng hẳn (Out đột xuất)
                IconButton(
                    onClick = {
                        isRunning = false
                        // TODO: Ghi nhận thất bại vào DB ở đây
                        onBack()
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Dừng", tint = Color(0xFFF44336), modifier = Modifier.size(32.dp))
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Nút Tạm dừng / Tiếp tục
                IconButton(
                    onClick = { isRunning = !isRunning },
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF212121), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Tạm dừng",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

// 🔥 HÀM GỬI THÔNG BÁO KHI OUT APP
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