package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroReportScreen(
    onBack: () -> Unit,
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val allRecords = pomodoroViewModel.allTimeRecords

    // 0: Tuần, 1: Tháng, 2: Tổng
    var selectedTab by remember { mutableIntStateOf(0) }
    // false: Tổng số, true: Trung bình mỗi ngày
    var isAverageMode by remember { mutableStateOf(false) }

    // Tính toán mốc thời gian lọc
    val now = Calendar.getInstance()

    val startOfWeek = now.clone() as Calendar
    startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
    startOfWeek.set(Calendar.HOUR_OF_DAY, 0); startOfWeek.set(Calendar.MINUTE, 0); startOfWeek.set(Calendar.SECOND, 0)

    val startOfMonth = now.clone() as Calendar
    startOfMonth.set(Calendar.DAY_OF_MONTH, 1)
    startOfMonth.set(Calendar.HOUR_OF_DAY, 0); startOfMonth.set(Calendar.MINUTE, 0); startOfMonth.set(Calendar.SECOND, 0)

    // Lọc dữ liệu theo Tab
    val filteredRecords = when (selectedTab) {
        0 -> allRecords.filter { it.startTime >= startOfWeek.timeInMillis }
        1 -> allRecords.filter { it.startTime >= startOfMonth.timeInMillis }
        else -> allRecords
    }

    // Các con số thô (Tổng)
    val totalSessions = filteredRecords.size
    val successSessions = filteredRecords.count { it.isCompleted }
    val failedSessions = filteredRecords.count { !it.isCompleted }
    val totalMinutes = filteredRecords.sumOf { it.actualFocusMinutes }

    // Tính số chia (Divider) để lấy Trung bình mỗi ngày
    val divider = when (selectedTab) {
        0 -> 7f // Trung bình tuần chia 7 ngày
        1 -> now.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat() // Trung bình tháng chia số ngày trong tháng
        else -> {
            if (allRecords.isEmpty()) 1f else {
                val firstRecordTime = allRecords.minOf { it.startTime }
                val days = ((now.timeInMillis - firstRecordTime) / (1000 * 60 * 60 * 24)).toFloat()
                if (days < 1f) 1f else days // Ít nhất là 1 ngày
            }
        }
    }

    // Dữ liệu sẽ hiển thị (Phụ thuộc vào nút Tổng / Trung bình)
    val displaySessions = if (isAverageMode) totalSessions / divider else totalSessions.toFloat()
    val displaySuccess = if (isAverageMode) successSessions / divider else successSessions.toFloat()
    val displayFailed = if (isAverageMode) failedSessions / divider else failedSessions.toFloat()
    val displayMinutes = if (isAverageMode) totalMinutes / divider else totalMinutes.toFloat()

    // Hàm format hiển thị (Nếu là trung bình thì hiện 1 chữ số thập phân, VD: 2.5)
    val formatVal = { value: Float ->
        if (isAverageMode) String.format("%.1f", value) else value.toInt().toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo cáo Pomodoro", fontWeight = FontWeight.Bold) },
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
        ) {
            // 🔥 MỤC 1: 3 TAB TUẦN / THÁNG / TỔNG
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Tuần này") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tháng này") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Toàn thời gian") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔥 MỤC 2: NÚT CHUYỂN TỔNG SỐ / TRUNG BÌNH
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFEEEEEE)
                ) {
                    Row {
                        Button(
                            onClick = { isAverageMode = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isAverageMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (!isAverageMode) Color.White else Color.Gray
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Tổng số") }

                        Button(
                            onClick = { isAverageMode = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAverageMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isAverageMode) Color.White else Color.Gray
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) { Text("Trung bình / Ngày") }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // 🔥 CARD 1: CÁC CHỈ SỐ CƠ BẢN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Số lần thực hiện", color = Color.Gray, fontSize = 14.sp)
                            Text("${formatVal(displaySessions)} lần", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Thời gian thực hiện", color = Color.Gray, fontSize = 14.sp)
                            Text("${formatVal(displayMinutes)} phút", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🔥 CARD 2: BIỂU ĐỒ TRÒN THÀNH CÔNG / THẤT BẠI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tỉ lệ Hoàn thành", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

                            // VẼ BIỂU ĐỒ TRÒN
                            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                if (totalSessions == 0) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(color = Color.LightGray)
                                    }
                                } else {
                                    // Tính góc của biểu đồ
                                    val successAngle = 360f * (successSessions.toFloat() / totalSessions.toFloat())
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        // Cung Thành công (Xanh lá)
                                        drawArc(color = Color(0xFF4CAF50), startAngle = -90f, sweepAngle = successAngle, useCenter = true)
                                        // Cung Thất bại (Đỏ)
                                        drawArc(color = Color(0xFFF44336), startAngle = -90f + successAngle, sweepAngle = 360f - successAngle, useCenter = true)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(32.dp))

                            // CHÚ THÍCH (LEGEND)
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Thành công: ${formatVal(displaySuccess)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFF44336), CircleShape))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Thất bại: ${formatVal(displayFailed)}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}