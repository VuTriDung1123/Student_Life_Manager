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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalyticsScreen(onBack: () -> Unit, viewModel: ExpenseViewModel = viewModel()) {
    val transactions = viewModel.filteredTransactions
    val categories = viewModel.categories
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    // Tính toán Tổng quan
    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense
    val totalFlow = totalIncome + totalExpense // Tổng dòng tiền để tính % Thu/Chi

    // Trạng thái để chuyển đổi biểu đồ cơ cấu danh mục
    var showIncomeChart by remember { mutableStateOf(false) }

    // Dữ liệu cho Biểu đồ danh mục
    val chartData = transactions
        .filter { it.isIncome == showIncomeChart }
        .groupBy { it.categoryId }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val chartTotal = if (showIncomeChart) totalIncome else totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Phân tích Tháng ${viewModel.selectedMonth + 1}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // --- 1. THẺ TỔNG QUAN ---
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Thu nhập", style = MaterialTheme.typography.labelMedium)
                            Text(formatter.format(totalIncome), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Chi tiêu", style = MaterialTheme.typography.labelMedium)
                            Text(formatter.format(totalExpense), fontWeight = FontWeight.Bold, color = Color(0xFFF44336), fontSize = 18.sp)
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tiết kiệm được:", fontWeight = FontWeight.Bold)
                        Text(formatter.format(balance), fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            // --- 2. BIỂU ĐỒ TỶ LỆ THU / CHI (TÍNH NĂNG MỚI THEO YÊU CẦU) ---
            item {
                Text("Tỷ lệ Thu / Chi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))

                if (totalFlow == 0.0) {
                    Text("Chưa có dòng tiền nào trong tháng.", color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Vòng tròn tỷ lệ
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val incomeSweep = (totalIncome / totalFlow).toFloat() * 360f
                                val expenseSweep = (totalExpense / totalFlow).toFloat() * 360f

                                // Vẽ cung Thu nhập (Màu xanh)
                                drawArc(
                                    color = Color(0xFF4CAF50),
                                    startAngle = -90f,
                                    sweepAngle = incomeSweep,
                                    useCenter = false,
                                    style = Stroke(width = 40f, cap = StrokeCap.Butt)
                                )
                                // Vẽ cung Chi tiêu (Màu đỏ)
                                drawArc(
                                    color = Color(0xFFF44336),
                                    startAngle = -90f + incomeSweep,
                                    sweepAngle = expenseSweep,
                                    useCenter = false,
                                    style = Stroke(width = 40f, cap = StrokeCap.Butt)
                                )
                            }
                            Text("VS", fontWeight = FontWeight.Black, color = Color.Gray, fontSize = 18.sp)
                        }

                        // Chú thích phần trăm bên phải
                        Column {
                            val incomePercent = (totalIncome / totalFlow) * 100
                            val expensePercent = (totalExpense / totalFlow) * 100

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(14.dp).background(Color(0xFF4CAF50), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thu: ${String.format("%.1f%%", incomePercent)}", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(14.dp).background(Color(0xFFF44336), CircleShape))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chi: ${String.format("%.1f%%", expensePercent)}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Divider(modifier = Modifier.padding(bottom = 24.dp))
            }

            // --- 3. BIỂU ĐỒ CƠ CẤU THEO DANH MỤC ---
            item {
                Text(
                    text = if (showIncomeChart) "Cơ cấu Danh mục Thu" else "Cơ cấu Danh mục Chi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(selected = !showIncomeChart, onClick = { showIncomeChart = false }, label = { Text("Chi tiêu") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2)))
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterChip(selected = showIncomeChart, onClick = { showIncomeChart = true }, label = { Text("Thu nhập") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9)))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                if (chartTotal == 0.0) {
                    Text("Chưa có dữ liệu để vẽ biểu đồ.", color = Color.Gray, modifier = Modifier.padding(32.dp))
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                        Canvas(modifier = Modifier.size(200.dp)) {
                            var startAngle = -90f
                            chartData.forEach { (categoryId, amount) ->
                                val category = categories.find { it.id == categoryId }
                                val sweepAngle = (amount / chartTotal).toFloat() * 360f
                                val color = try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")) } catch (e: Exception) { Color.LightGray }

                                drawArc(color = color, startAngle = startAngle, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = 60f, cap = StrokeCap.Butt))
                                startAngle += sweepAngle
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (showIncomeChart) "Tổng thu" else "Tổng chi", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            Text(text = formatter.format(chartTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = if (showIncomeChart) Color(0xFF4CAF50) else Color(0xFFF44336))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            // --- DANH SÁCH CHI TIẾT TỪNG DANH MỤC ---
            if (chartData.isNotEmpty()) {
                items(chartData) { (categoryId, amount) ->
                    val category = categories.find { it.id == categoryId }
                    val percentage = (amount / chartTotal) * 100

                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(16.dp).background(color = try { Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")) } catch (e: Exception) { Color.LightGray }, shape = CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(category?.name ?: "Khác", fontWeight = FontWeight.Bold)
                            Text(String.format("%.1f%%", percentage), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text(text = formatter.format(amount), fontWeight = FontWeight.Bold, color = if (showIncomeChart) Color(0xFF4CAF50) else Color(0xFFF44336))
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}