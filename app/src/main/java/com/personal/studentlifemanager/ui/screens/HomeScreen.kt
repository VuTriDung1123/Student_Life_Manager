package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout // Sửa icon logout
import androidx.compose.material.icons.filled.* // Import tất cả icon mặc định
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.studentlifemanager.ui.components.ModuleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    onLogout: () -> Unit,
    onNavigateToExpense: () -> Unit // Thêm dòng này
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Life Manager", fontWeight = FontWeight.ExtraBold) },
                actions = {
                    IconButton(onClick = onLogout) {
                        // Dùng AutoMirrored để icon quay đúng hướng
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color.Red)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(text = "Xin chào,", fontSize = 14.sp)
            Text(text = "$userName ✨", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sử dụng các Icon cơ bản có sẵn trong thư viện Default để tránh lỗi
                item {
                    ModuleCard(
                        title = "Chi tiêu",
                        icon = Icons.Default.ShoppingCart,
                        color = Color(0xFF4CAF50)
                    ) {
                        onNavigateToExpense() // Khi bấm nút thì gọi hàm này
                    }
                }
                item { ModuleCard("Pomodoro", Icons.Default.Schedule, Color(0xFFFF5722)) {} }
                item { ModuleCard("Thẻ nhớ", Icons.Default.MenuBook, Color(0xFF2196F3)) {} }
                item { ModuleCard("Thói quen", Icons.Default.TaskAlt, Color(0xFF9C27B0)) {} }
                item(span = { GridItemSpan(2) }) {
                    ModuleCard("UTH Helper (Bản đồ & Cẩm nang)", Icons.Default.Place, Color(0xFFFFC107)) {}
                }
            }
        }
    }
}