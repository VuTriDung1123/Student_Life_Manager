package com.personal.studentlifemanager.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroRecord
import com.personal.studentlifemanager.service.PomodoroService
import com.personal.studentlifemanager.worker.PomodoroReminderWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit,
    onNavigateToTimer: (PomodoroConfig, String) -> Unit,
    onNavigateToReport: () -> Unit,
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("PomodoroPrefs", Context.MODE_PRIVATE)

    var config by remember {
        mutableStateOf(
            PomodoroConfig(
                focusTime = sharedPreferences.getInt("focus_time", 25),
                shortBreak = sharedPreferences.getInt("short_break", 5),
                sessionsCount = sharedPreferences.getInt("sessions_count", 4),
                longBreak = sharedPreferences.getInt("long_break", 15),
                autoStart = sharedPreferences.getBoolean("auto_start", true),
                soundEnabled = sharedPreferences.getBoolean("sound_enabled", true),
                keepScreenOn = sharedPreferences.getBoolean("keep_screen_on", true),
                hardcoreMode = sharedPreferences.getBoolean("hardcore_mode", false)
            )
        )
    }

    var taskNameInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val currentDateRecords = pomodoroViewModel.currentDateRecords

    val activeTimeLeft by PomodoroService.timeLeft.collectAsState()
    LaunchedEffect(Unit) {
        if (PomodoroService.timeLeft.value > 0L) {
            onNavigateToTimer(config, PomodoroService.currentTaskName)
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                pomodoroViewModel.fetchRecordsForSelectedDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pomodoro", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Tuần: ${pomodoroViewModel.weeklyTotalMinutes} phút", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("🔥 Chuỗi: ${pomodoroViewModel.currentStreak} ngày", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5722))
                    }
                    IconButton(onClick = onNavigateToReport) {
                        Icon(Icons.Default.Assessment, contentDescription = "Báo cáo", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. CARD CÀI ĐẶT & BẮT ĐẦU
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tập trung / Nghỉ ngắn / Số phiên / Nghỉ dài", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${config.focusTime} / ${config.shortBreak} / ${config.sessionsCount} / ${config.longBreak}", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = taskNameInput,
                        onValueChange = { taskNameInput = it },
                        label = { Text("Bạn định làm gì trong phiên này?") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // 🔥 ĐÃ ĐƯA BỘ HẸN GIỜ VÀO ĐÚNG VỊ TRÍ
                    val calendar = Calendar.getInstance()
                    val timePickerDialog = TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val targetTime = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                set(Calendar.MINUTE, minute)
                                set(Calendar.SECOND, 0)
                            }
                            if (targetTime.before(Calendar.getInstance())) {
                                targetTime.add(Calendar.DAY_OF_YEAR, 1)
                            }

                            val delay = targetTime.timeInMillis - System.currentTimeMillis()

                            val inputData = Data.Builder().putString("taskName", taskNameInput.ifBlank { "Tự do" }).build()
                            val workRequest = OneTimeWorkRequestBuilder<PomodoroReminderWorker>()
                                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                                .setInputData(inputData)
                                .build()

                            WorkManager.getInstance(context).enqueue(workRequest)

                            val timeStr = String.format("%02d:%02d", hourOfDay, minute)
                            Toast.makeText(context, "Đã hẹn giờ cày cuốc lúc $timeStr", Toast.LENGTH_SHORT).show()
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    )

                    // DÀN 3 NÚT BẤM (Cấu hình, Hẹn giờ, Bắt đầu)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier.background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp)).size(50.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Cấu hình", tint = Color.Gray)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { timePickerDialog.show() },
                            modifier = Modifier.background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp)).size(50.dp)
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = "Lên lịch", tint = Color(0xFF1976D2))
                        }

                        Button(
                            onClick = {
                                PomodoroService.sendCommand(context, "ACTION_START", config, taskNameInput)
                                onNavigateToTimer(config, taskNameInput)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).weight(1f).padding(start = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                        ) {
                            Text("Bắt đầu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // 2. ĐIỀU HƯỚNG NGÀY THÁNG
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(pomodoroViewModel.selectedDate.time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { pomodoroViewModel.changeDate(-1) }) { Icon(Icons.Default.ChevronLeft, "Hôm trước") }
                Text(dateStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp))
                IconButton(onClick = { pomodoroViewModel.changeDate(1) }) { Icon(Icons.Default.ChevronRight, "Hôm sau") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 4. THỐNG KÊ CƠ BẢN CỦA NGÀY & ĐIỂM NĂNG SUẤT
            val totalCompleted = currentDateRecords.count { it.isCompleted }
            val totalMinutes = currentDateRecords.filter { it.isCompleted }.sumOf { it.actualFocusMinutes }
            val score = pomodoroViewModel.productivityScore // Lấy điểm năng suất

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lịch sử phiên", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))

                    // Hiển thị Điểm Năng Suất (Màu sắc thay đổi theo điểm)
                    val scoreColor = when {
                        score >= 80 -> Color(0xFF4CAF50) // Xanh lá: Xuất sắc
                        score >= 50 -> Color(0xFFFF9800) // Cam: Khá
                        else -> Color(0xFFF44336) // Đỏ: Cần cố gắng
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = scoreColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "⚡ Điểm: $score/100",
                            color = scoreColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "$totalCompleted phiên • $totalMinutes phút",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentDateRecords.isEmpty()) {
                    item { Text("Chưa có phiên tập trung nào trong ngày này.", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp)) }
                } else {
                    items(currentDateRecords) { record -> RecordItem(record, pomodoroViewModel) }
                }
            }
        }
    }

    if (showSettingsDialog) {
        PomodoroSettingsDialog(
            currentConfig = config,
            onDismiss = { showSettingsDialog = false },
            onSave = { newConfig ->
                config = newConfig
                showSettingsDialog = false
                sharedPreferences.edit()
                    .putInt("focus_time", newConfig.focusTime)
                    .putInt("short_break", newConfig.shortBreak)
                    .putInt("sessions_count", newConfig.sessionsCount)
                    .putInt("long_break", newConfig.longBreak)
                    .putBoolean("auto_start", newConfig.autoStart)
                    .putBoolean("sound_enabled", newConfig.soundEnabled)
                    .putBoolean("keep_screen_on", newConfig.keepScreenOn)
                    .putBoolean("hardcore_mode", newConfig.hardcoreMode)
                    .apply()
            }
        )
    }
}

@Composable
fun PomodoroSettingsDialog(
    currentConfig: PomodoroConfig,
    onDismiss: () -> Unit,
    onSave: (PomodoroConfig) -> Unit
) {
    val context = LocalContext.current
    var focusTime by remember { mutableStateOf(currentConfig.focusTime.toString()) }
    var shortBreak by remember { mutableStateOf(currentConfig.shortBreak.toString()) }
    var sessionsCount by remember { mutableStateOf(currentConfig.sessionsCount.toString()) }
    var longBreak by remember { mutableStateOf(currentConfig.longBreak.toString()) }

    var autoStart by remember { mutableStateOf(currentConfig.autoStart) }
    var soundEnabled by remember { mutableStateOf(currentConfig.soundEnabled) }
    var keepScreenOn by remember { mutableStateOf(currentConfig.keepScreenOn) }
    var hardcoreMode by remember { mutableStateOf(currentConfig.hardcoreMode) }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cài đặt Pomodoro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigInputField("Tập trung", focusTime, { focusTime = it }, Modifier.weight(1f))
                    ConfigInputField("Nghỉ ngắn", shortBreak, { shortBreak = it }, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigInputField("Số phiên", sessionsCount, { sessionsCount = it }, Modifier.weight(1f))
                    ConfigInputField("Nghỉ dài", longBreak, { longBreak = it }, Modifier.weight(1f))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // CÁC CÔNG TẮC UX
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tự động chuyển phiên", fontSize = 14.sp); Switch(checked = autoStart, onCheckedChange = { autoStart = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Âm thanh & Rung", fontSize = 14.sp); Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Giữ sáng màn hình", fontSize = 14.sp); Switch(checked = keepScreenOn, onCheckedChange = { keepScreenOn = it })
                }

                // KỶ LUẬT THÉP
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Kỷ luật thép (Chặn thông báo)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                        Text("Thoát app sẽ bị hủy phiên!", fontSize = 10.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = hardcoreMode,
                        onCheckedChange = { isChecked ->
                            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                            if (isChecked && !notificationManager.isNotificationPolicyAccessGranted) {
                                // Xin quyền DND nếu chưa có
                                val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            } else {
                                hardcoreMode = isChecked
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD32F2F), checkedTrackColor = Color(0xFFFFCDD2))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val newConfig = PomodoroConfig(
                            focusTime.toIntOrNull() ?: 25, shortBreak.toIntOrNull() ?: 5, sessionsCount.toIntOrNull() ?: 4, longBreak.toIntOrNull() ?: 15,
                            autoStart, soundEnabled, keepScreenOn, hardcoreMode
                        )
                        onSave(newConfig)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)), shape = RoundedCornerShape(12.dp)
                ) { Text("Lưu cấu hình", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun RecordItem(record: PomodoroRecord, pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val calendar = Calendar.getInstance().apply { timeInMillis = record.startTime }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val sessionName = when (hour) { in 0..11 -> "Buổi sáng"; in 12..17 -> "Buổi chiều"; else -> "Buổi tối" }
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(record.startTime)
    val statusColor = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusIcon = if (record.isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel

    // ĐÃ XÓA SẠCH TIMEPICKER KHỎI ĐÂY 🚀

    val configString = "${record.configFocus}/${record.configShort}/${record.configSessions}/${record.configLong}"

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (record.taskName.isNotEmpty()) record.taskName else "Tự do", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)

                Text("$timeString ($sessionName) • [Cấu hình: $configString]", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))

                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(if (record.isCompleted) "Hoàn thành" else "Thất bại", color = statusColor, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(" • ${record.actualFocusMinutes} phút", fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 6.dp))
                }
            }

            var showDeleteConfirm by remember { mutableStateOf(false) }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(Icons.Default.Cancel, contentDescription = "Xóa", tint = Color.LightGray)
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("Xóa phiên này?") },
                    text = { Text("Dữ liệu sau khi xóa sẽ không thể khôi phục.") },
                    confirmButton = { TextButton(onClick = { showDeleteConfirm = false; pomodoroViewModel.deleteRecord(record.id) }) { Text("Xóa", color = Color.Red) } },
                    dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Hủy") } }
                )
            }
        }
    }
}

@Composable
fun ConfigInputField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) onValueChange(it) },
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
    )
}