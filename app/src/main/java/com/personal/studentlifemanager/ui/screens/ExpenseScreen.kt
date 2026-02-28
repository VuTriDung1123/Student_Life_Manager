package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý chi tiêu") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* Mở form thêm chi tiêu */ }) {
                Icon(Icons.Default.Add, contentDescription = "Thêm")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Tổng chi tiêu tháng này:", style = MaterialTheme.typography.labelLarge)
            Text("2.500.000đ", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(20.dp))

            Text("Lịch sử giao dịch", style = MaterialTheme.typography.titleMedium)
            // Sau này sẽ dùng LazyColumn để hiện danh sách lấy từ Room
        }
    }
}