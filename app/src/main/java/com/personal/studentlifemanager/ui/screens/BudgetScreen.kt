package com.personal.studentlifemanager.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.Budget
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(onBack: () -> Unit, viewModel: ExpenseViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<Budget?>(null) }

    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    // Chỉ lấy ngân sách của tháng hiện tại
    val currentBudgets = viewModel.budgets.filter {
        it.month == viewModel.selectedMonth && it.year == viewModel.selectedYear
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ngân sách Tháng ${viewModel.selectedMonth + 1}", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingBudget = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Thêm ngân sách")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (currentBudgets.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có ngân sách nào. Hãy tạo hạn mức chi tiêu nhé!", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()) {
                    items(currentBudgets) { budget ->
                        val category = viewModel.categories.find { it.id == budget.categoryId }

                        // 🔥 LOGIC TÍNH TOÁN TIỀN ĐÃ TIÊU CHO DANH MỤC NÀY
                        val spent = viewModel.filteredTransactions
                            .filter { it.categoryId == budget.categoryId && !it.isIncome && !it.isTransfer }
                            .sumOf { it.amount }

                        val progress = if (budget.amountLimit > 0) (spent / budget.amountLimit).toFloat() else 0f

                        // 🔥 ĐỔI MÀU THÔNG MINH
                        val progressColor = when {
                            progress < 0.5f -> Color(0xFF4CAF50) // Dưới 50% -> Xanh lá (An toàn)
                            progress < 0.8f -> Color(0xFFFFC107) // Dưới 80% -> Vàng (Cảnh báo nhẹ)
                            progress <= 1.0f -> Color(0xFFFF9800) // Gần mức -> Cam (Nguy hiểm)
                            else -> Color(0xFFF44336) // Vượt mức -> Đỏ
                        }

                        val animatedProgress by animateFloatAsState(
                            targetValue = progress.coerceAtMost(1f), // Thanh đầy max 100%
                            animationSpec = tween(1000)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).background(try { Color(android.graphics.Color.parseColor(category?.colorHex)) } catch(e:Exception){Color.Gray}, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(category?.name?.take(1) ?: "?", fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(category?.name ?: "Danh mục bị xóa", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))

                                    IconButton(onClick = { editingBudget = budget; showDialog = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Edit, null, tint = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(onClick = { viewModel.deleteBudget(budget.id) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, null, tint = Color.Red.copy(alpha=0.6f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Đã chi: ${formatter.format(spent)}", style = MaterialTheme.typography.bodyMedium, color = if(progress > 1f) Color.Red else Color.Unspecified)
                                    Text("Hạn mức: ${formatter.format(budget.amountLimit)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // THANH PROGRESS BAR MƯỢT MÀ
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                                    color = progressColor,
                                    trackColor = Color.LightGray.copy(alpha = 0.5f)
                                )

                                if (progress >= 1f) {
                                    Text("⚠️ Bạn đã vượt ngân sách!", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddBudgetDialog(
                viewModel = viewModel,
                editingBudget = editingBudget,
                onDismiss = { showDialog = false },
                onSave = { categoryId, limit ->
                    viewModel.saveBudget(categoryId, limit) { showDialog = false }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(viewModel: ExpenseViewModel, editingBudget: Budget?, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    // Chỉ chọn các danh mục Chi tiêu
    val expenseCategories = viewModel.categories.filter { !it.isIncome }

    var selectedCategory by remember { mutableStateOf(expenseCategories.find { it.id == editingBudget?.categoryId } ?: expenseCategories.firstOrNull()) }
    var amount by remember { mutableStateOf(if (editingBudget != null) String.format(Locale.US, "%.0f", editingBudget.amountLimit) else "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if(editingBudget == null) "Tạo ngân sách" else "Sửa ngân sách") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Chọn danh mục",
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        expenseCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = { selectedCategory = cat; expanded = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Hạn mức (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = amount.toDoubleOrNull() ?: 0.0
                if (selectedCategory != null && limit > 0) onSave(selectedCategory!!.id, limit)
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}