package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.Transaction
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalyticsScreen(onBack: () -> Unit, viewModel: ExpenseViewModel = viewModel()) {
    val allTransactions = viewModel.allTransactions
    val categories = viewModel.categories
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tháng này", "Năm này", "Tổng")

    val filteredData = remember(selectedTabIndex, allTransactions) {
        val now = Calendar.getInstance()
        when (selectedTabIndex) {
            0 -> allTransactions.filter { t ->
                val cal = Calendar.getInstance().apply { timeInMillis = t.date }
                cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) && cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            }
            1 -> allTransactions.filter { t ->
                val cal = Calendar.getInstance().apply { timeInMillis = t.date }
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            }
            else -> allTransactions
        }
    }

    val totalIncome = filteredData.filter { it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val totalExpense = filteredData.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val totalFlow = totalIncome + totalExpense

    // --- TÍNH TOÁN SO SÁNH THÁNG TRƯỚC (NEW) ---
    var prevMonth = viewModel.selectedMonth - 1
    var prevYear = viewModel.selectedYear
    if (prevMonth < 0) { prevMonth = 11; prevYear -= 1 }
    val lastMonthExpense = viewModel.getExpenseForMonth(prevMonth, prevYear)
    val diffExpense = totalExpense - lastMonthExpense
    val diffPercent = if (lastMonthExpense > 0) (diffExpense / lastMonthExpense) * 100 else 0.0

    // --- DỮ LIỆU BIỂU ĐỒ CỘT 6 THÁNG (NEW) ---
    val trendData = (5 downTo 0).map { offset ->
        var m = viewModel.selectedMonth - offset
        var y = viewModel.selectedYear
        if (m < 0) { m += 12; y -= 1 }
        Pair("T${m + 1}", viewModel.getExpenseForMonth(m, y))
    }
    val maxTrendExpense = trendData.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1.0

    var showIncomeDetail by remember { mutableStateOf(false) }

    val chartData: List<Pair<String, Double>> = filteredData
        .filter { it.isIncome == showIncomeDetail && !it.isTransfer }
        .groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val chartTotal = if (showIncomeDetail) totalIncome else totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo cáo tài chính", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title -> Tab(selected = selectedTabIndex == index, onClick = { selectedTabIndex = index }, text = { Text(title) }) }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                // --- 1. SO SÁNH THÁNG TRƯỚC (HIỆN KHI Ở TAB "THÁNG NÀY") ---
                if (selectedTabIndex == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (diffExpense > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(if (diffExpense > 0) Color(0xFFF44336) else Color(0xFF4CAF50), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(if (diffExpense > 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, null, tint = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(if (diffExpense > 0) "Tiêu nhiều hơn tháng trước" else "Tiết kiệm hơn tháng trước", fontWeight = FontWeight.Bold)
                                    Text("${formatter.format(Math.abs(diffExpense))} (${String.format("%.1f", Math.abs(diffPercent))}%)", color = if (diffExpense > 0) Color(0xFFF44336) else Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- 1.5. DỰ ĐOÁN CHI TIÊU CUỐI THÁNG (FORECASTING) ---
                if (selectedTabIndex == 0) {
                    item {
                        val predictedExpense = viewModel.getPredictedEndOfMonthExpense()
                        val isDanger = predictedExpense > totalIncome && totalIncome > 0

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDanger) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Insights, contentDescription = "Dự báo", tint = if (isDanger) Color(0xFFF44336) else MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Dự báo AI cuối tháng", fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Với tốc độ chi tiêu hiện tại, ước tính tổng chi tháng này của bạn sẽ là:", fontSize = 13.sp, color = Color.Gray)
                                Text(formatter.format(predictedExpense), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (isDanger) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface)

                                if (isDanger) {
                                    Text("⚠️ Cảnh báo: Bạn có nguy cơ tiêu vượt quá tổng thu nhập!", color = Color(0xFFF44336), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // --- 2. BIỂU ĐỒ XU HƯỚNG CHI TIÊU 6 THÁNG (BAR CHART) ---
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Xu hướng 6 tháng gần nhất", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    val currentExpense = viewModel.getExpenseForMonth(viewModel.selectedMonth, viewModel.selectedYear)

                    // 🔥 SỬA Ở ĐÂY: Rút cái màu (Composable) ra ngoài Canvas
                    val primaryColor = MaterialTheme.colorScheme.primary

                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 8.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = size.width / (trendData.size * 2)
                            var xOffset = barWidth / 2

                            trendData.forEach { (label, amount) ->
                                val barHeight = (amount / maxTrendExpense).toFloat() * size.height

                                // Gọi biến màu đã rút ra ở trên
                                drawRoundRect(
                                    color = primaryColor.copy(alpha = if (amount == currentExpense) 1f else 0.5f),
                                    topLeft = Offset(xOffset, size.height - barHeight),
                                    size = Size(barWidth, barHeight),
                                    cornerRadius = CornerRadius(8f, 8f)
                                )
                                xOffset += barWidth * 2
                            }
                        }
                    }
                    // Chú thích tháng dưới biểu đồ
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        trendData.forEach { (label, _) ->
                            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
                }

                // --- 3. TỶ LỆ DÒNG TIỀN (DONUT CHART GỐC) ---
                item {
                    Text("Tỷ lệ dòng tiền", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    if (totalFlow == 0.0) {
                        Text("Không có dữ liệu.", color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                                Canvas(modifier = Modifier.size(100.dp)) {
                                    val incomeSweep = (totalIncome / totalFlow).toFloat() * 360f
                                    drawArc(Color(0xFF4CAF50), -90f, incomeSweep, false, style = Stroke(40f))
                                    drawArc(Color(0xFFF44336), -90f + incomeSweep, 360f - incomeSweep, false, style = Stroke(40f))
                                }
                                Text("VS", fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                            Column {
                                LegendItem(Color(0xFF4CAF50), "Thu", (totalIncome/totalFlow*100))
                                Spacer(modifier = Modifier.height(8.dp))
                                LegendItem(Color(0xFFF44336), "Chi", (totalExpense/totalFlow*100))
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(bottom = 24.dp))
                }

                // --- 4. TOP DANH MỤC CHI TIÊU ---
                item {
                    Text("Phân tích chi tiết", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.padding(vertical = 16.dp)) {
                        FilterChip(selected = !showIncomeDetail, onClick = { showIncomeDetail = false }, label = { Text("Chi tiêu") })
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(selected = showIncomeDetail, onClick = { showIncomeDetail = true }, label = { Text("Thu nhập") })
                    }
                }

                // Biểu đồ danh mục (Giữ nguyên)
                item {
                    if (chartTotal > 0) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp).padding(bottom = 16.dp)) {
                            Canvas(modifier = Modifier.size(180.dp)) {
                                var start = -90f
                                chartData.forEach { dataPair ->
                                    val catId = dataPair.first
                                    val category = categories.find { it.id == catId }
                                    val sweep = (dataPair.second / chartTotal).toFloat() * 360f
                                    val color = try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")) } catch (e: Exception) { Color.LightGray }
                                    drawArc(color, start, sweep, false, style = Stroke(50f))
                                    start += sweep
                                }
                            }
                            Text(formatter.format(chartTotal), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Danh sách top danh mục (Có ghim 🥇🥈🥉 cho Top 3)
                items(chartData.withIndex().toList()) { (index, dataPair) ->
                    val category = categories.find { it.id == dataPair.first }
                    val medal = when(index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> ""
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(16.dp).background(
                            try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")) } catch (e: Exception) { Color.Gray }, CircleShape
                        ))
                        Text("${category?.name ?: "Khác"} $medal", modifier = Modifier.padding(start = 12.dp).weight(1f), fontWeight = if (index < 3) FontWeight.Bold else FontWeight.Normal)
                        Text(formatter.format(dataPair.second), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

//// Helper function để lấy tiền tháng hiện tại tô đậm cột Bar Chart
//@Composable
//fun currentMonthExpense(viewModel: ExpenseViewModel): Double {
//    return viewModel.getExpenseForMonth(viewModel.selectedMonth, viewModel.selectedYear)
//}

@Composable
fun LegendItem(color: Color, label: String, percent: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label: ${String.format("%.1f%%", percent)}", fontSize = 14.sp)
    }
}