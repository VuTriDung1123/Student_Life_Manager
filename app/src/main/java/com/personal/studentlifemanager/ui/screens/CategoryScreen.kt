package com.personal.studentlifemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(onBack: () -> Unit, viewModel: ExpenseViewModel = viewModel()) {
    var showAddDialog by remember { mutableStateOf(false) }

    // 0: Chi tiêu, 1: Thu nhập
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Tự động lọc danh sách theo Tab đang chọn
    val displayCategories = viewModel.categories.filter { it.isIncome == (selectedTabIndex == 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý danh mục", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm danh mục", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- THANH TAB THU / CHI ---
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Chi tiêu", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Thu nhập", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // --- DANH SÁCH DANH MỤC ---
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (displayCategories.isEmpty()) {
                    item {
                        Text(
                            text = "Chưa có danh mục nào. Hãy tạo mới nhé!",
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                } else {
                    items(displayCategories) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Vòng tròn màu sắc
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch(e: Exception) { Color.LightGray },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cat.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = if (cat.isIncome) "Dùng cho Thu nhập" else "Dùng cho Chi tiêu",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                // Nút xóa danh mục
                                IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Red.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOG THÊM DANH MỤC ---
        if (showAddDialog) {
            AddCategoryDialog(
                initialIsIncome = (selectedTabIndex == 1), // Tự động chọn đúng loại Tab đang xem
                onDismiss = { showAddDialog = false },
                onSave = { name, colorHex, isIncome ->
                    viewModel.addCategory(name, colorHex, isIncome) { showAddDialog = false }
                }
            )
        }
    }
}

@Composable
fun AddCategoryDialog(
    initialIsIncome: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(initialIsIncome) }

    // Bảng màu Pastel chuẩn phong cách ứng dụng
    val colorPalette = listOf(
        "#FFB6C1", "#ADD8E6", "#98FB98", "#FFD700",
        "#DDA0DD", "#FFA07A", "#20B2AA", "#87CEFA",
        "#FFC0CB", "#E6E6FA", "#F0E68C", "#B0E0E6"
    )
    var selectedColor by remember { mutableStateOf(colorPalette[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm danh mục mới", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Chi tiêu") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Thu nhập") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên danh mục (vd: Mua sắm, Lương...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Chọn màu sắc đại diện:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // Vẽ lưới màu 4 cột x 3 hàng
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (col in 0..3) {
                                val index = row * 4 + col
                                if (index < colorPalette.size) {
                                    val hex = colorPalette[index]
                                    ColorBox(hex, selectedColor == hex) { selectedColor = hex }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, selectedColor, isIncome) }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
fun ColorBox(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.LightGray }
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(color, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(modifier = Modifier.size(18.dp).background(Color.White, CircleShape))
        }
    }
}