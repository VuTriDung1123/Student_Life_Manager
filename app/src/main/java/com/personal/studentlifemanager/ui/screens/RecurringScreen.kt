package com.personal.studentlifemanager.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.RecurringExpense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(onBack: () -> Unit, viewModel: ExpenseViewModel = viewModel()) {
    var showDialog by remember { mutableStateOf(false) }
    val recurringList = viewModel.recurringExpenses
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giao dịch định kỳ", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Thêm định kỳ", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (recurringList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Chưa có giao dịch tự động nào.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()) {
                    item {
                        Text("Hệ thống sẽ tự động tạo giao dịch vào ngày đến hạn. Bạn có thể bật/tắt tạm thời bằng công tắc bên dưới.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                    }
                    items(recurringList) { recurring ->
                        val category = viewModel.categories.find { it.id == recurring.categoryId }
                        val wallet = viewModel.wallets.find { it.id == recurring.walletId }
                        val nextDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(recurring.nextExecutionTime)

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = if (recurring.isActive) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).background(try { Color(android.graphics.Color.parseColor(category?.colorHex)) } catch(e:Exception){Color.Gray}, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(category?.name?.take(1) ?: "?", fontWeight = FontWeight.Bold, color = if (recurring.isActive) Color.Unspecified else Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(category?.name ?: "Khác", fontWeight = FontWeight.Bold, color = if (recurring.isActive) Color.Unspecified else Color.Gray)
                                    Text(recurring.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("Từ ví: ${wallet?.name}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("Kỳ tới: $nextDateStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${if(recurring.isIncome) "+" else "-"}${formatter.format(recurring.amount)}",
                                        color = if (!recurring.isActive) Color.Gray else if (recurring.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Switch(
                                            checked = recurring.isActive,
                                            onCheckedChange = { viewModel.toggleRecurringState(recurring) },
                                            modifier = Modifier.scale(0.8f)
                                        )
                                        IconButton(onClick = { viewModel.deleteRecurring(recurring.id) }) {
                                            Icon(Icons.Default.Delete, "Xóa", tint = Color.Red.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddRecurringDialog(viewModel) { showDialog = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringDialog(viewModel: ExpenseViewModel, onDismiss: () -> Unit) {
    var isIncome by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val availableCategories = viewModel.categories.filter { it.isIncome == isIncome }
    var selectedCategory by remember(isIncome, availableCategories) { mutableStateOf(availableCategories.firstOrNull()) }

    val wallets = viewModel.wallets
    var selectedWallet by remember(wallets) { mutableStateOf(wallets.firstOrNull()) }

    val context = LocalContext.current
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDateMillis)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, y: Int, m: Int, d: Int -> val cal = Calendar.getInstance(); cal.set(y, m, d); selectedDateMillis = cal.timeInMillis },
        Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm chu kỳ mới", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Chi tiêu") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2)))
                    FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Thu nhập") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9)))
                }

                Text("Nguồn tiền (Ví):", style = MaterialTheme.typography.labelMedium)
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(wallets) { w -> FilterChip(selected = selectedWallet?.id == w.id, onClick = { selectedWallet = w }, label = { Text(w.name) }) }
                }

                Text("Danh mục:", style = MaterialTheme.typography.labelMedium)
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableCategories) { cat -> FilterChip(selected = selectedCategory?.id == cat.id, onClick = { selectedCategory = cat }, label = { Text(cat.name) }) }
                }

                OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Kỳ thực thi đầu tiên: $dateString", color = MaterialTheme.colorScheme.onSurface)
                }

                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Số tiền mỗi kỳ") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú (Tiền trọ, Netflix...)") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                if (parsedAmount > 0 && selectedCategory != null && selectedWallet != null) {
                    val recurring = RecurringExpense(
                        amount = parsedAmount,
                        note = note,
                        categoryId = selectedCategory!!.id,
                        walletId = selectedWallet!!.id,
                        isIncome = isIncome,
                        nextExecutionTime = selectedDateMillis,
                        isActive = true
                    )
                    viewModel.saveRecurring(recurring, onDismiss)
                }
            }) { Text("Lưu tự động") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}