package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroReportScreen(
    onBack: () -> Unit,
    pomodoroViewModel: PomodoroViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var currentMainTab by remember { mutableIntStateOf(0) } // 0: Thống kê, 1: Thành tựu

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
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // ĐIỀU HƯỚNG CHÍNH (THỐNG KÊ / THÀNH TỰU)
            TabRow(selectedTabIndex = currentMainTab) {
                Tab(selected = currentMainTab == 0, onClick = { currentMainTab = 0 }, text = { Text("📊 Thống kê") })
                Tab(selected = currentMainTab == 1, onClick = { currentMainTab = 1 }, text = { Text("🏆 Thành tựu") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (currentMainTab == 0) {
                // TRANG 1: THỐNG KÊ (Biểu đồ tròn)
                AnalyticsView(pomodoroViewModel)
            } else {
                // TRANG 2: GAMIFICATION (Huy hiệu)
                GamificationView(pomodoroViewModel)
            }
        }
    }
}

// ==========================================
// TRANG 1: THỐNG KÊ BIỂU ĐỒ (Giữ nguyên giao diện bạn đã ưng ý)
// ==========================================
@Composable
fun AnalyticsView(pomodoroViewModel: PomodoroViewModel) {
    val allRecords = pomodoroViewModel.allTimeRecords
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tuần, 1: Tháng, 2: Tổng
    var isAverageMode by remember { mutableStateOf(false) }

    val now = Calendar.getInstance()
    val startOfWeek = now.clone() as Calendar
    startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek); startOfWeek.set(Calendar.HOUR_OF_DAY, 0); startOfWeek.set(Calendar.MINUTE, 0); startOfWeek.set(Calendar.SECOND, 0)
    val startOfMonth = now.clone() as Calendar
    startOfMonth.set(Calendar.DAY_OF_MONTH, 1); startOfMonth.set(Calendar.HOUR_OF_DAY, 0); startOfMonth.set(Calendar.MINUTE, 0); startOfMonth.set(Calendar.SECOND, 0)

    val filteredRecords = when (selectedTab) {
        0 -> allRecords.filter { it.startTime >= startOfWeek.timeInMillis }
        1 -> allRecords.filter { it.startTime >= startOfMonth.timeInMillis }
        else -> allRecords
    }

    val totalSessions = filteredRecords.size
    val successSessions = filteredRecords.count { it.isCompleted }
    val failedSessions = filteredRecords.count { !it.isCompleted }
    val totalMinutes = filteredRecords.sumOf { it.actualFocusMinutes }

    val divider = when (selectedTab) {
        0 -> 7f
        1 -> now.getActualMaximum(Calendar.DAY_OF_MONTH).toFloat()
        else -> {
            if (allRecords.isEmpty()) 1f else {
                val days = ((now.timeInMillis - allRecords.minOf { it.startTime }) / (1000 * 60 * 60 * 24)).toFloat()
                if (days < 1f) 1f else days
            }
        }
    }

    val displaySessions = if (isAverageMode) totalSessions / divider else totalSessions.toFloat()
    val displaySuccess = if (isAverageMode) successSessions / divider else successSessions.toFloat()
    val displayFailed = if (isAverageMode) failedSessions / divider else failedSessions.toFloat()
    val displayMinutes = if (isAverageMode) totalMinutes / divider else totalMinutes.toFloat()

    val formatVal = { value: Float -> if (isAverageMode) String.format("%.1f", value) else value.toInt().toString() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFEEEEEE)) {
                Row {
                    Button(onClick = { selectedTab = 0 }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (selectedTab == 0) Color.White else Color.Gray), shape = RoundedCornerShape(24.dp)) { Text("Tuần") }
                    Button(onClick = { selectedTab = 1 }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (selectedTab == 1) Color.White else Color.Gray), shape = RoundedCornerShape(24.dp)) { Text("Tháng") }
                    Button(onClick = { selectedTab = 2 }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (selectedTab == 2) Color.White else Color.Gray), shape = RoundedCornerShape(24.dp)) { Text("Tổng") }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFFEEEEEE)) {
                Row {
                    Button(onClick = { isAverageMode = false }, colors = ButtonDefaults.buttonColors(containerColor = if (!isAverageMode) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (!isAverageMode) Color.White else Color.Gray), shape = RoundedCornerShape(24.dp)) { Text("Tổng số") }
                    Button(onClick = { isAverageMode = true }, colors = ButtonDefaults.buttonColors(containerColor = if (isAverageMode) MaterialTheme.colorScheme.primary else Color.Transparent, contentColor = if (isAverageMode) Color.White else Color.Gray), shape = RoundedCornerShape(24.dp)) { Text("Trung bình/Ngày") }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Thực hiện", color = Color.Gray, fontSize = 14.sp); Text("${formatVal(displaySessions)} lần", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) }
                    Column(horizontalAlignment = Alignment.End) { Text("Thời gian", color = Color.Gray, fontSize = 14.sp); Text("${formatVal(displayMinutes)} phút", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00)) }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tỉ lệ Hoàn thành", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                            if (totalSessions == 0) Canvas(modifier = Modifier.fillMaxSize()) { drawCircle(color = Color.LightGray) }
                            else {
                                val successAngle = 360f * (successSessions.toFloat() / totalSessions.toFloat())
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(color = Color(0xFF4CAF50), startAngle = -90f, sweepAngle = successAngle, useCenter = true)
                                    drawArc(color = Color(0xFFF44336), startAngle = -90f + successAngle, sweepAngle = 360f - successAngle, useCenter = true)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50), CircleShape)); Spacer(modifier = Modifier.width(8.dp)); Text("Thành công: ${formatVal(displaySuccess)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32)) }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(12.dp).background(Color(0xFFF44336), CircleShape)); Spacer(modifier = Modifier.width(8.dp)); Text("Thất bại: ${formatVal(displayFailed)}", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TRANG 2: THÀNH TỰU VÀ MỤC TIÊU
// ==========================================
@Composable
fun GamificationView(pomodoroViewModel: PomodoroViewModel) {
    val weeklyGoal = pomodoroViewModel.weeklyGoalSessions
    val currentProgress = pomodoroViewModel.currentWeeklySessions
    val progressRatio = (currentProgress.toFloat() / weeklyGoal.toFloat()).coerceIn(0f, 1f)

    val badges = pomodoroViewModel.unlockedBadges

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // MỤC TIÊU TUẦN
        Text("Mục tiêu Tuần", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hoàn thành: $currentProgress / $weeklyGoal phiên", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                    Text("${(progressRatio * 100).toInt()}%", fontWeight = FontWeight.ExtraBold, color = Color(0xFFF57C00))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier.fillMaxWidth().height(12.dp),
                    color = Color(0xFFFF9800),
                    trackColor = Color(0xFFFFE0B2),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                if (currentProgress >= weeklyGoal) {
                    Text("🎉 Bạn đã đạt mục tiêu tuần này! Tuyệt vời!", fontSize = 12.sp, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // BỘ SƯU TẬP HUY HIỆU
        val unlockedCount = badges.count { it.isUnlocked }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Bộ sưu tập Huy hiệu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("$unlockedCount / ${badges.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(badges) { badge ->
                BadgeItem(badge)
            }
        }
    }
}

@Composable
fun BadgeItem(badge: PomodoroViewModel.PomodoroBadge) {
    val bgColor = if (badge.isUnlocked) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
    val iconAlpha = if (badge.isUnlocked) 1f else 0.3f
    val textColor = if (badge.isUnlocked) Color(0xFF1565C0) else Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(badge.icon, fontSize = 32.sp, modifier = Modifier.alpha(iconAlpha))
            Spacer(modifier = Modifier.height(8.dp))
            Text(badge.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor, textAlign = TextAlign.Center, lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(badge.desc, fontSize = 9.sp, color = Color.DarkGray, textAlign = TextAlign.Center, modifier = Modifier.alpha(iconAlpha), lineHeight = 11.sp)
        }
    }
}