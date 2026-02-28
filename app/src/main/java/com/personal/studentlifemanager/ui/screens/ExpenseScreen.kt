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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.Transaction
import com.personal.studentlifemanager.data.repository.ExpenseRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    onBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToCategory: () -> Unit = {},
    viewModel: ExpenseViewModel = viewModel()
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    // 🔥 ĐIỂM ĂN TIỀN: Lấy danh sách đã bị "lọc" theo tháng thay vì lấy toàn bộ
    val transactions = viewModel.filteredTransactions

    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    val totalIncome = transactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý chi tiêu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại") }
                },
                actions = {
                    IconButton(onClick = onNavigateToCategory) { Icon(Icons.Default.List, contentDescription = "Danh mục", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onNavigateToAnalytics) { Icon(Icons.Default.Analytics, contentDescription = "Báo cáo", tint = MaterialTheme.colorScheme.primary) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {

            // 🔥 THANH ĐIỀU HƯỚNG THÁNG / NĂM
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Tháng trước")
                }

                // Hiển thị Tháng (Tháng trong Calendar bắt đầu từ 0 nên phải + 1)
                Text(
                    text = "Tháng ${viewModel.selectedMonth + 1}, ${viewModel.selectedYear}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Tháng sau")
                }
            }

            // CARD TỔNG QUAN
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Số dư trong tháng", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(
                        text = formatter.format(balance),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("Thu nhập", style = MaterialTheme.typography.labelMedium)
                            Text(formatter.format(totalIncome), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Chi tiêu", style = MaterialTheme.typography.labelMedium)
                            Text(formatter.format(totalExpense), fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Lịch sử giao dịch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Text("Không có giao dịch nào trong tháng này.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(transactions) { transaction ->
                        TransactionItem(transaction, viewModel, formatter)
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AddTransactionForm(viewModel = viewModel) { showBottomSheet = false }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, viewModel: ExpenseViewModel, formatter: NumberFormat) {
    // ĐÃ SỬA: dùng viewModel.categories thay vì defaultCategories
    val category = viewModel.categories.find { it.id == transaction.categoryId }
    val amountColor = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val sign = if (transaction.isIncome) "+" else "-"

    val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaction.date)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(category?.name?.take(1) ?: "?", fontWeight = FontWeight.Bold, color = Color.DarkGray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category?.name ?: "Khác", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (transaction.note.isNotEmpty()) {
                    Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "$sign${formatter.format(transaction.amount)}", color = amountColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                // Nút xóa
                IconButton(onClick = { viewModel.deleteTransaction(transaction.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun AddTransactionForm(viewModel: ExpenseViewModel, onSaved: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }

    // ĐÃ SỬA: dùng viewModel.categories thay vì defaultCategories
    val availableCategories = viewModel.categories.filter { it.isIncome == isIncome }
    var selectedCategory by remember(isIncome, availableCategories) { mutableStateOf(availableCategories.firstOrNull()) }

    // Xử lý Lịch
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedDateMillis by remember { mutableStateOf(calendar.timeInMillis) }
    val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDateMillis)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            selectedDateMillis = newCalendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ghi chép giao dịch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            FilterChip(
                selected = !isIncome, onClick = { isIncome = false }, label = { Text("Chi tiêu") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2))
            )
            Spacer(modifier = Modifier.width(16.dp))
            FilterChip(
                selected = isIncome, onClick = { isIncome = true }, label = { Text("Thu nhập") },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Danh mục:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(availableCategories) { cat ->
                FilterChip(
                    selected = selectedCategory?.id == cat.id,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.LightGray }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Ngày giao dịch: $dateString", color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = amount, onValueChange = { amount = it }, label = { Text("Số tiền (VNĐ)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = note, onValueChange = { note = it }, label = { Text("Ghi chú (Tô bún bò, Mua vở...)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                if (parsedAmount > 0 && selectedCategory != null) {
                    val transaction = Transaction(
                        amount = parsedAmount,
                        note = note,
                        date = selectedDateMillis,
                        categoryId = selectedCategory!!.id,
                        isIncome = isIncome
                    )
                    ExpenseRepository().addTransaction(transaction, onSuccess = onSaved, onFailure = {})
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Lưu giao dịch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}