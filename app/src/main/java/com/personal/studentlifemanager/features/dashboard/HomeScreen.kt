package com.personal.studentlifemanager.features.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout // Sửa icon logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.* // Import tất cả icon mặc định
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.personal.studentlifemanager.core.components.ModuleCard
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userName: String,
    userProfileViewModel: UserProfileViewModel = viewModel(),
    onLogout: () -> Unit,
    onNavigateToExpense: () -> Unit,
    onNavigateToPomodoro: () -> Unit,
    onNavigateToFlashcard: () -> Unit,
    onNavigateToHabit: () -> Unit,
    onNavigateToCampus: () -> Unit
) {
    val userProfile = userProfileViewModel.userProfile
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Xin chào,", fontSize = 14.sp)
                    Text(text = "$userName ✨", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                
                // Gamification UI
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Level ${userProfile.level}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = "${userProfile.currentExp} / ${userProfile.nextLevelExp} EXP",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    LinearProgressIndicator(
                        progress = { if (userProfile.nextLevelExp > 0) userProfile.currentExp.toFloat() / userProfile.nextLevelExp else 0f },
                        modifier = Modifier
                            .width(100.dp)
                            .height(8.dp)
                            .padding(top = 4.dp),
                        color = Color(0xFFFF9800),
                        trackColor = Color(0xFFFFF3E0),
                    )
                }
            }

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
                item {
                    ModuleCard(
                        title = "Pomodoro",
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFFF5722)
                    ) {
                        onNavigateToPomodoro()
                    }
                }
                item { ModuleCard("Thẻ nhớ", Icons.AutoMirrored.Filled.MenuBook, Color(0xFF2196F3)) {
                    onNavigateToFlashcard()
                } }
                item { ModuleCard("Thói quen", Icons.Default.TaskAlt, Color(0xFF9C27B0)) {
                    onNavigateToHabit()
                } }
                item(span = { GridItemSpan(2) }) {
                    ModuleCard("Campus Helper", Icons.Default.Place, Color(0xFFFFC107)) {
                        onNavigateToCampus()
                    }
                }
            }
        }
    }
}
