package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroReportScreen(
    onBack: () -> Unit,
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val avgLength = pomodoroViewModel.averageFocusLength
    val bestTime = pomodoroViewModel.bestTimeOfDay
    val bestDay = pomodoroViewModel.mostProductiveDay
    val taskStats = pomodoroViewModel.allTimeTaskStats

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo cáo Năng suất", fontWeight = FontWeight.Bold) },
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

            // 🔥 PHẦN 1: INSIGHTS (Thấu hiểu thói quen)
            Text("Thấu hiểu Thói quen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Thẻ Thời lượng trung bình
                Card(
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF1976D2))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tr.bình phiên", fontSize = 12.sp, color = Color.Gray)
                        Text("$avgLength phút", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1565C0))
                    }
                }

                // Thẻ Khung giờ vàng
                Card(
                    modifier = Modifier.weight(1f).height(110.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFF57C00))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Khung giờ vàng", fontSize = 12.sp, color = Color.Gray)
                        Text(bestTime, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFFE65100))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thẻ Ngày năng suất nhất
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Ngày năng suất nhất tuần", fontSize = 14.sp, color = Color.Gray)
                        Text(bestDay, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF4A148C))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 PHẦN 2: TIẾN ĐỘ THEO TASK (Toàn thời gian)
            Text("Phân bổ Thời gian theo Task (All-time)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            if (taskStats.isEmpty()) {
                Text("Chưa có dữ liệu task nào được hoàn thành.", color = Color.Gray, fontSize = 14.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(taskStats.entries.toList().sortedByDescending { it.value.second }) { entry -> // Sắp xếp theo phút giảm dần
                        val taskName = entry.key
                        val pomodoroCount = entry.value.first
                        val totalMinutes = entry.value.second

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = Color(0xFF388E3C))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(taskName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$totalMinutes phút", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF1B5E20))
                                    Text("$pomodoroCount phiên", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}