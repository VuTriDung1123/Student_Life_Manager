package com.personal.studentlifemanager.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroRecord
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit,
    onNavigateToTimer: (PomodoroConfig, String) -> Unit, // Đã thêm tham số String cho Task
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
                autoStart = sharedPreferences.getBoolean("auto_start", true)
            )
        )
    }

    var taskNameInput by remember { mutableStateOf("") }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val todayRecords = pomodoroViewModel.todayRecords

    // Bắt buộc tải lại dữ liệu khi quay lại màn hình
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                pomodoroViewModel.fetchTodayRecords()
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

                    // 🔥 Ô NHẬP TÊN CÔNG VIỆC
                    OutlinedTextField(
                        value = taskNameInput,
                        onValueChange = { taskNameInput = it },
                        label = { Text("Bạn định làm gì trong phiên này?") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(
                            onClick = { showSettingsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Cấu hình", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cấu hình")
                        }

                        Button(
                            onClick = { onNavigateToTimer(config, taskNameInput) }, // Truyền cả Cấu hình và Tên Task
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).width(120.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                        ) {
                            Text("Bắt đầu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Nhật ký hôm nay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            // 🔥 HIỂN THỊ THỐNG KÊ CƠ BẢN
            val totalCompleted = todayRecords.count { it.isCompleted }
            val totalMinutes = todayRecords.filter { it.isCompleted }.sumOf { it.actualFocusMinutes }
            Text(
                text = "Đã hoàn thành: $totalCompleted phiên • Tổng thời gian: $totalMinutes phút",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (todayRecords.isEmpty()) {
                    item { Text("Chưa có phiên tập trung nào hôm nay. Hãy bắt đầu ngay!", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp)) }
                } else {
                    items(todayRecords) { record -> RecordItem(record) }
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
    var focusTime by remember { mutableStateOf(currentConfig.focusTime.toString()) }
    var shortBreak by remember { mutableStateOf(currentConfig.shortBreak.toString()) }
    var sessionsCount by remember { mutableStateOf(currentConfig.sessionsCount.toString()) }
    var longBreak by remember { mutableStateOf(currentConfig.longBreak.toString()) }
    var autoStart by remember { mutableStateOf(currentConfig.autoStart) } // Thêm state này

    val presets = listOf(PomodoroConfig(25, 5, 4, 15), PomodoroConfig(30, 5, 4, 15), PomodoroConfig(45, 10, 4, 20))

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cài đặt Pomodoro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Cancel, "Đóng", tint = Color.Gray) }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigInputField("Tập trung", focusTime, { focusTime = it }, Modifier.weight(1f))
                    ConfigInputField("Nghỉ ngắn", shortBreak, { shortBreak = it }, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigInputField("Số phiên", sessionsCount, { sessionsCount = it }, Modifier.weight(1f))
                    ConfigInputField("Nghỉ dài", longBreak, { longBreak = it }, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 🔥 CÔNG TẮC BẬT TẮT AUTO-START
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tự động bắt đầu chuyển phiên", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = autoStart, onCheckedChange = { autoStart = it })
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    presets.forEach { preset ->
                        val isSelected = preset.focusTime.toString() == focusTime && preset.shortBreak.toString() == shortBreak && preset.sessionsCount.toString() == sessionsCount && preset.longBreak.toString() == longBreak
                        Surface(
                            onClick = { focusTime = preset.focusTime.toString(); shortBreak = preset.shortBreak.toString(); sessionsCount = preset.sessionsCount.toString(); longBreak = preset.longBreak.toString() },
                            shape = RoundedCornerShape(8.dp), color = if (isSelected) Color(0xFF4CAF50) else Color(0xFFE8F5E9), modifier = Modifier.height(36.dp)
                        ) { Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text("${preset.focusTime}/${preset.shortBreak}/${preset.sessionsCount}/${preset.longBreak}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF2E7D32)) } }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        val newConfig = PomodoroConfig(focusTime.toIntOrNull() ?: 25, shortBreak.toIntOrNull() ?: 5, sessionsCount.toIntOrNull() ?: 4, longBreak.toIntOrNull() ?: 15, autoStart)
                        onSave(newConfig)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)), shape = RoundedCornerShape(12.dp)
                ) { Text("Lưu cấu hình", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// Thay đổi tham số hàm RecordItem để nhận lệnh Xóa
@Composable
fun RecordItem(record: PomodoroRecord, pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val calendar = Calendar.getInstance().apply { timeInMillis = record.startTime }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val sessionName = when (hour) { in 0..11 -> "Buổi sáng"; in 12..17 -> "Buổi chiều"; else -> "Buổi tối" }
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(record.startTime)
    val statusColor = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusIcon = if (record.isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (record.taskName.isNotEmpty()) record.taskName else "Tự do", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("$timeString ($sessionName)", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                    Text(if (record.isCompleted) "Hoàn thành" else "Thất bại", color = statusColor, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(" • ${record.actualFocusMinutes} phút", fontSize = 14.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 6.dp))
                }
            }

            // 🔥 NÚT XÓA BÊN PHẢI
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