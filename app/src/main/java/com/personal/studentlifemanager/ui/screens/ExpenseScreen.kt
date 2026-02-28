package com.personal.studentlifemanager.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.personal.studentlifemanager.data.model.Transaction
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(onBack: () -> Unit, viewModel: ExpenseViewModel = viewModel()) {
    var showBottomSheet by remember { mutableStateOf(false) }

    // Kéo dữ liệu từ ViewModel ra
    val transactions = viewModel.transactions

    // Tự động tính toán tổng số tiền chi tiêu
    val totalExpense = transactions.filter { !it.isIncome }.sumOf { it.amount }

    // Định dạng tiền tệ VNĐ cho đẹp
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý chi tiêu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()) {

            // CARD HIỂN THỊ TỔNG TIỀN
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Tổng chi tiêu tháng này", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatter.format(totalExpense),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Lịch sử giao dịch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // DANH SÁCH LỊCH SỬ GIAO DỊCH
            if (transactions.isEmpty()) {
                Text("Chưa có giao dịch nào.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
            } else {
                LazyColumn {
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
                AddTransactionForm(viewModel = viewModel) {
                    showBottomSheet = false
                }
            }
        }
    }
}

// KHUNG HIỂN THỊ 1 DÒNG GIAO DỊCH
@Composable
fun TransactionItem(transaction: Transaction, viewModel: ExpenseViewModel, formatter: NumberFormat) {
    val category = viewModel.defaultCategories.find { it.id == transaction.categoryId }
    val amountColor = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336)
    val sign = if (transaction.isIncome) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hình tròn đại diện danh mục
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
            }

            Text(
                text = "$sign${formatter.format(transaction.amount)}",
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

// ... (Đoạn hàm AddTransactionForm bên dưới giữ nguyên y hệt như code cũ của bạn)
@Composable
fun AddTransactionForm(viewModel: ExpenseViewModel, onSaved: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(viewModel.defaultCategories.first()) }

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
                if (parsedAmount > 0) {
                    viewModel.addTransaction(parsedAmount, note, selectedCategory.id, isIncome, onSaved)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Lưu giao dịch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}