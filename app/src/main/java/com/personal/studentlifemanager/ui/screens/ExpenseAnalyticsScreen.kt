package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAnalyticsScreen(onBack: () -> Unit, viewModel: ExpenseViewModel = viewModel()) {
    val transactions = viewModel.transactions
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val total = totalIncome + totalExpense

    // Tính tỷ lệ % để vẽ thanh biểu đồ (chống lỗi chia cho 0)
    val incomeWeight = if (total > 0) (totalIncome / total).toFloat() else 0.5f
    val expenseWeight = if (total > 0) (totalExpense / total).toFloat() else 0.5f

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
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            Text("Tỷ lệ Thu / Chi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // VẼ THANH BIỂU ĐỒ TRỰC QUAN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(15.dp))
            ) {
                if (totalIncome > 0) {
                    Box(modifier = Modifier.weight(incomeWeight).fillMaxHeight().background(Color(0xFF4CAF50)))
                }
                if (totalExpense > 0) {
                    Box(modifier = Modifier.weight(expenseWeight).fillMaxHeight().background(Color(0xFFF44336)))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CHI TIẾT
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🟢 Tổng thu nhập:", fontWeight = FontWeight.Bold)
                Text(formatter.format(totalIncome), color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🔴 Tổng chi tiêu:", fontWeight = FontWeight.Bold)
                Text(formatter.format(totalExpense), color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("💰 Tiết kiệm được:", fontWeight = FontWeight.Bold)
                Text(formatter.format(totalIncome - totalExpense), style = MaterialTheme.typography.titleLarge)
            }

            // Nhà tuyển dụng rất thích thấy các phân tích (Analytics) kiểu này trong CV đó!
        }
    }
}