package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroRecord
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun PomodoroSettingsDialog(
    currentConfig: PomodoroConfig,
    onDismiss: () -> Unit,
    onSave: (PomodoroConfig) -> Unit
) {
    // Tạo state lưu trữ tạm thời các giá trị người dùng đang gõ
    var focusTime by remember { mutableStateOf(currentConfig.focusTime.toString()) }
    var shortBreak by remember { mutableStateOf(currentConfig.shortBreak.toString()) }
    var sessionsCount by remember { mutableStateOf(currentConfig.sessionsCount.toString()) }
    var longBreak by remember { mutableStateOf(currentConfig.longBreak.toString()) }

    // Danh sách các cấu hình nhanh (Presets)
    val presets = listOf(
        PomodoroConfig(25, 5, 4, 15),
        PomodoroConfig(30, 5, 4, 15),
        PomodoroConfig(45, 10, 4, 20)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tiêu đề & Nút tắt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cài đặt Pomodoro", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Cancel, contentDescription = "Đóng", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Lưới 2x2 nhập thông số
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigInputField(label = "Tập trung", value = focusTime, onValueChange = { focusTime = it }, modifier = Modifier.weight(1f))
                    ConfigInputField(label = "Nghỉ ngắn", value = shortBreak, onValueChange = { shortBreak = it }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConfigInputField(label = "Số phiên", value = sessionsCount, onValueChange = { sessionsCount = it }, modifier = Modifier.weight(1f))
                    ConfigInputField(label = "Nghỉ dài", value = longBreak, onValueChange = { longBreak = it }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Các nút cấu hình nhanh
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    presets.forEach { preset ->
                        val isSelected = preset.focusTime.toString() == focusTime &&
                                preset.shortBreak.toString() == shortBreak &&
                                preset.sessionsCount.toString() == sessionsCount &&
                                preset.longBreak.toString() == longBreak

                        Surface(
                            onClick = {
                                focusTime = preset.focusTime.toString()
                                shortBreak = preset.shortBreak.toString()
                                sessionsCount = preset.sessionsCount.toString()
                                longBreak = preset.longBreak.toString()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF4CAF50) else Color(0xFFE8F5E9),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(modifier = Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${preset.focusTime}/${preset.shortBreak}/${preset.sessionsCount}/${preset.longBreak}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Nút Lưu
                Button(
                    onClick = {
                        // Chuyển chuỗi thành số, nếu người dùng gõ bậy thì lấy giá trị mặc định
                        val newConfig = PomodoroConfig(
                            focusTime = focusTime.toIntOrNull() ?: 25,
                            shortBreak = shortBreak.toIntOrNull() ?: 5,
                            sessionsCount = sessionsCount.toIntOrNull() ?: 4,
                            longBreak = longBreak.toIntOrNull() ?: 15
                        )
                        onSave(newConfig)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lưu cấu hình", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Widget phụ: Ô nhập số liệu
@Composable
fun ConfigInputField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            // Chỉ cho phép nhập số
            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                onValueChange(newValue)
            }
        },
        label = { Text(label, fontSize = 12.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit,
    onNavigateToTimer: () -> Unit
) {
    var config by remember { mutableStateOf(PomodoroConfig(25, 5, 4, 15)) }

    // 🔥 1. Biến điều khiển bật/tắt Popup
    var showSettingsDialog by remember { mutableStateOf(false) }

    val todayRecords = remember {
        listOf(
            PomodoroRecord(durationMinutes = 25, isCompleted = true),
            PomodoroRecord(durationMinutes = 25, isCompleted = false)
        )
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

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(
                            onClick = { showSettingsDialog = true }, // 🔥 2. Bấm nút này để mở Popup
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Cấu hình", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cấu hình")
                        }

                        Button(
                            onClick = onNavigateToTimer,
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

    // 🔥 3. HIỂN THỊ POPUP NẾU BIẾN BẰNG TRUE
    if (showSettingsDialog) {
        PomodoroSettingsDialog(
            currentConfig = config,
            onDismiss = { showSettingsDialog = false },
            onSave = { newConfig ->
                config = newConfig
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun RecordItem(record: PomodoroRecord) {
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(record.startTime)
    val statusColor = if (record.isCompleted) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusIcon = if (record.isCompleted) Icons.Default.CheckCircle else Icons.Default.Cancel
    val statusText = if (record.isCompleted) "Hoàn thành" else "Thất bại"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Phiên ${record.durationMinutes} phút", fontWeight = FontWeight.Bold)
                    Text(timeString, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}