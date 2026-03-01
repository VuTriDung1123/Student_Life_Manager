package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroScreen(
    onBack: () -> Unit,
    onNavigateToTimer: () -> Unit // Chuyển sang màn hình Đếm ngược
) {
    // Tạm thời dùng state giả lập để dựng UI
    var config by remember { mutableStateOf(PomodoroConfig(25, 5, 4, 15)) }

    // Giả lập lịch sử hôm nay (Sau này sẽ đọc từ Database)
    val todayRecords = remember {
        listOf(
            PomodoroRecord(durationMinutes = 25, isCompleted = true), // Hoàn thành
            PomodoroRecord(durationMinutes = 25, isCompleted = false) // Thất bại
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

            // 🔥 THẺ 1: CHẾ ĐỘ TẬP TRUNG (Khu vực cốt lõi)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tập trung / Nghỉ ngắn / Số phiên / Nghỉ dài",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Hiển thị thông số 25 / 5 / 4 / 15
                    Text(
                        text = "${config.focusTime} / ${config.shortBreak} / ${config.sessionsCount} / ${config.longBreak}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { /* TODO: Mở popup cài đặt */ },
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)) // Màu cam năng động
                        ) {
                            Text("Bắt đầu", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🔥 THẺ 2: NHẬT KÝ HÔM NAY (Thay thế cho chọn hạt giống)
            Text("Nhật ký hôm nay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (todayRecords.isEmpty()) {
                    item {
                        Text("Chưa có phiên tập trung nào hôm nay. Hãy bắt đầu ngay!", color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(top = 16.dp))
                    }
                } else {
                    items(todayRecords) { record ->
                        RecordItem(record)
                    }
                }
            }
        }
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