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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    val allTransactions: List<Transaction> = viewModel.allTransactions
    val categories = viewModel.categories
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Tháng này", "Năm này", "Tổng")

    // Lọc dữ liệu dựa trên Tab
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

    val totalIncome = filteredData.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = filteredData.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val totalFlow = totalIncome + totalExpense

    var showIncomeDetail by remember { mutableStateOf(false) }

    // Dữ liệu cho Biểu đồ cơ cấu danh mục - Ép kiểu rõ ràng để tránh lỗi "it"
    val chartData: List<Pair<String, Double>> = filteredData
        .filter { it.isIncome == showIncomeDetail }
        .groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val chartTotal = if (showIncomeDetail) totalIncome else totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo cáo tài chính", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Tổng Thu", style = MaterialTheme.typography.labelMedium)
                                    Text(formatter.format(totalIncome), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 18.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Tổng Chi", style = MaterialTheme.typography.labelMedium)
                                    Text(formatter.format(totalExpense), fontWeight = FontWeight.Bold, color = Color(0xFFF44336), fontSize = 18.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(modifier = Modifier.alpha(0.3f)) // Sửa Divider -> HorizontalDivider
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Số dư thực tế:", style = MaterialTheme.typography.labelMedium)
                            Text(formatter.format(balance), fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        }
                    }
                }

                item {
                    Text("Tỷ lệ Thu / Chi (Dòng tiền)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))

                    if (totalFlow == 0.0) {
                        Text("Không có dữ liệu dòng tiền.", color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
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

                item {
                    Text("Cơ cấu danh mục", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.padding(vertical = 16.dp)) {
                        FilterChip(selected = !showIncomeDetail, onClick = { showIncomeDetail = false }, label = { Text("Chi tiêu") })
                        Spacer(modifier = Modifier.width(12.dp))
                        FilterChip(selected = showIncomeDetail, onClick = { showIncomeDetail = true }, label = { Text("Thu nhập") })
                    }
                }

                item {
                    if (chartTotal > 0) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp).padding(bottom = 16.dp)) {
                            Canvas(modifier = Modifier.size(180.dp)) {
                                var start = -90f
                                chartData.forEach { dataPair ->
                                    val catId = dataPair.first
                                    val amt = dataPair.second
                                    val category = categories.find { it.id == catId }
                                    val sweep = (amt / chartTotal).toFloat() * 360f
                                    val color = try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")) } catch (e: Exception) { Color.LightGray }
                                    drawArc(color, start, sweep, false, style = Stroke(50f))
                                    start += sweep
                                }
                            }
                            Text(formatter.format(chartTotal), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(chartData) { dataPair ->
                    val catId = dataPair.first
                    val amt = dataPair.second
                    val category = categories.find { it.id == catId }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(
                            try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")) } catch (e: Exception) { Color.Gray }, CircleShape
                        ))
                        Text(category?.name ?: "Khác", modifier = Modifier.padding(start = 8.dp).weight(1f))
                        Text(formatter.format(amt), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, percent: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$label: ${String.format("%.1f%%", percent)}", fontSize = 14.sp)
    }
}