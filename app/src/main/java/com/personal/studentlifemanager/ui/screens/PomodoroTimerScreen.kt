package com.personal.studentlifemanager.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.personal.studentlifemanager.R
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroPhase // Lấy từ thư mục model
import com.personal.studentlifemanager.service.PomodoroService

// 🔥 FIX LỖI "getValue": Thêm 2 thư viện này để đọc dữ liệu từ StateFlow của Service
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

// 🔥 1. HÀM TÀI XẾ: Chuyên chở lệnh từ Nút bấm xuống Service chạy ngầm
fun sendPomodoroCommand(context: Context, action: String, config: PomodoroConfig? = null, taskName: String? = null) {
    val intent = Intent(context, PomodoroService::class.java).apply {
        this.action = action
        if (config != null) {
            putExtra("focus", config.focusTime)
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

@Composable
fun PomodoroTimerScreen(
    config: PomodoroConfig,
    taskName: String,
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // 🔥 2. CẮM ỐNG HÚT DỮ LIỆU: Đọc trực tiếp từ Service
    val timeLeft by PomodoroService.timeLeft.collectAsState()
    val isRunning by PomodoroService.isRunning.collectAsState()
    val currentPhase by PomodoroService.currentPhase.collectAsState()
    val currentSession by PomodoroService.currentSession.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    // Bắt đầu Service khi vừa mở màn hình lên (Nếu chưa chạy)
    LaunchedEffect(Unit) {
        if (!isRunning && timeLeft == 0L) {
            sendPomodoroCommand(context, "ACTION_START", config, taskName)
        }
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
                        sendPomodoroCommand(context, "ACTION_PAUSE")
                        showExitDialog = true
                    },
                    modifier = Modifier.size(64.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(16.dp))
                ) { Icon(Icons.Default.Stop, contentDescription = "Dừng", tint = Color(0xFFF44336), modifier = Modifier.size(32.dp)) }

                Spacer(modifier = Modifier.width(32.dp))

                IconButton(
                    onClick = {
                        if (isRunning) sendPomodoroCommand(context, "ACTION_PAUSE")
                        else sendPomodoroCommand(context, "ACTION_RESUME")
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
                sendPomodoroCommand(context, "ACTION_RESUME")
            },
            title = { Text("Dừng Pomodoro?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn muốn làm gì với phiên này?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        sendPomodoroCommand(context, "ACTION_STOP")
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Bỏ cuộc", color = Color.White, fontSize = 12.sp) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showExitDialog = false
                        sendPomodoroCommand(context, "ACTION_STOP")
                        onBack()
                    }) { Text("Hủy (Không lưu)", color = Color.Gray, fontSize = 12.sp) }

                    TextButton(onClick = {
                        showExitDialog = false
                        sendPomodoroCommand(context, "ACTION_RESUME")
                    }) { Text("Tiếp tục", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
                }
            }
        )
    }
}