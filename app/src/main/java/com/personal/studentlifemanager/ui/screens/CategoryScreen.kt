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
    val categories = viewModel.categories

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý danh mục", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Thêm danh mục", tint = Color.White)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            items(categories) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(cat.colorHex)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat.name.take(1), fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(if (cat.isIncome) "Thu nhập" else "Chi tiêu", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                            Icon(Icons.Default.Delete, "Xóa", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCategoryDialog(
                onDismiss = { showAddDialog = false },
                onSave = { name, colorHex, isIncome ->
                    viewModel.addCategory(name, colorHex, isIncome) { showAddDialog = false }
                }
            )
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onSave: (String, String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    // Bảng màu Pastel ngọt ngào (Sakura pink, Mint, Lavender...)
    val colorPalette = listOf("#FFB6C1", "#ADD8E6", "#98FB98", "#FFD700", "#DDA0DD", "#FFA07A", "#20B2AA", "#87CEFA")
    var selectedColor by remember { mutableStateOf(colorPalette[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm danh mục mới", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Chi tiêu") })
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Thu nhập") })
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên danh mục") }, singleLine = true)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Chọn màu sắc:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                // Vẽ lưới màu
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colorPalette.take(4).forEach { hex ->
                        ColorBox(hex, selectedColor == hex) { selectedColor = hex }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    colorPalette.drop(4).forEach { hex ->
                        ColorBox(hex, selectedColor == hex) { selectedColor = hex }
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
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(modifier = Modifier.size(16.dp).background(Color.White, CircleShape))
        }
    }
}