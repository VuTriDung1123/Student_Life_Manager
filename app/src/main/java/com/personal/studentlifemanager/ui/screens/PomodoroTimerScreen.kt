package com.personal.studentlifemanager.ui.screens

import android.app.Activity
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.personal.studentlifemanager.R
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroPhase
import com.personal.studentlifemanager.service.PomodoroService

@Composable
fun PomodoroTimerScreen(
    config: PomodoroConfig,
    taskName: String,
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val timeLeft by PomodoroService.timeLeft.collectAsState()
    val isRunning by PomodoroService.isRunning.collectAsState()
    val currentPhase by PomodoroService.currentPhase.collectAsState()
    val currentSession by PomodoroService.currentSession.collectAsState()
    val isFinished by PomodoroService.isFinished.collectAsState() // Đọc tín hiệu báo Xong

    var showExitDialog by remember { mutableStateOf(false) }

    // 🔥 TỰ ĐỘNG ĐÁ VĂNG RA NGOÀI KHI HOÀN THÀNH LONG BREAK
    LaunchedEffect(isFinished) {
        if (isFinished) {
            PomodoroService.isFinished.value = false // Reset lại
            onBack()
        }
    }

    /// 🔥 LUẬT MỚI: BẤT KỂ CHẾ ĐỘ NÀO, CỨ THOÁT APP LÀ HỦY PHIÊN
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            // BỎ ĐIỀU KIỆN HARDCORE MODE. Cứ ON_STOP (ẩn màn hình) lúc đang Tập Trung là chết!
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP && currentPhase == PomodoroPhase.FOCUS) {
                PomodoroService.sendCommand(context, "ACTION_ABORT")
                onBack()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Chặn phím Back
    BackHandler(enabled = (timeLeft > 0L)) {
        // Cứ vuốt Back là hiện hộp thoại Bỏ cuộc
        PomodoroService.sendCommand(context, "ACTION_PAUSE")
        showExitDialog = true
    }

    var bgmSelection by remember { mutableIntStateOf(0) }
    val bgmList = listOf(Pair("Tắt nhạc", 0), Pair("Mưa & Sấm chớp", R.raw.rain_thunderstorm), Pair("Sóng biển", R.raw.sea_wave), Pair("Rừng tuyết", R.raw.snow_falling_tree))
    var bgmPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.navigationBars())
        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.navigationBars())
            bgmPlayer?.release()
        }
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

    val timeString = String.format("%02d:%02d", timeLeft / 60, timeLeft % 60)
    val bgColor = when(currentPhase) {
        PomodoroPhase.FOCUS -> MaterialTheme.colorScheme.background
        PomodoroPhase.SHORT_BREAK -> Color(0xFFE8F5E9)
        PomodoroPhase.LONG_BREAK -> Color(0xFFE3F2FD)
    }

    DisposableEffect(Unit) {
        val window = (context as Activity).window
        if (config.keepScreenOn) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
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
                        PomodoroService.sendCommand(context, "ACTION_PAUSE")
                        showExitDialog = true
                    },
                    modifier = Modifier.size(64.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                ) { Icon(Icons.Default.Stop, contentDescription = "Dừng", tint = Color(0xFFF44336), modifier = Modifier.size(32.dp)) }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(
                    onClick = {
                        if (isRunning) PomodoroService.sendCommand(context, "ACTION_PAUSE")
                        else PomodoroService.sendCommand(context, "ACTION_RESUME")
                    },
                    modifier = Modifier.size(80.dp).background(Color(0xFF212121), CircleShape)
                ) { Icon(imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Tạm dừng", tint = Color.White, modifier = Modifier.size(40.dp)) }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
                PomodoroService.sendCommand(context, "ACTION_RESUME")
            },
            title = { Text("Dừng Pomodoro?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn muốn làm gì với phiên này?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        PomodoroService.sendCommand(context, "ACTION_ABORT")
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Bỏ cuộc (Lưu thất bại)", color = Color.White, fontSize = 12.sp) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        PomodoroService.sendCommand(context, "ACTION_STOP")
                        onBack()
                    }) { Text("Hủy (Không lưu)", color = Color.Gray, fontSize = 12.sp) }

                    TextButton(onClick = {
                        showExitDialog = false
                        PomodoroService.sendCommand(context, "ACTION_RESUME")
                    }) { Text("Tiếp tục", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                }
            }
        )
    }
}