package com.personal.studentlifemanager.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    onBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToCategory: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    // 🔥 BIẾN QUAN TRỌNG: Lưu giao dịch đang được chọn để sửa (null = đang thêm mới)
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val transactions = viewModel.filteredTransactions
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý chi tiêu", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = onNavigateToCategory) { Icon(Icons.Default.List, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onNavigateToAnalytics) { Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingTransaction = null // Reset về thêm mới
                showBottomSheet = true
            }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            // Thanh chuyển tháng (Giữ nguyên code cũ)
            MonthNavigation(viewModel)

            // Ô TÌM KIẾM (MỚI)
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                placeholder = { Text("Tìm kiếm ghi chú...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        viewModel = viewModel,
                        formatter = formatter,
                        onClick = {
                            editingTransaction = transaction // Chọn để sửa
                            showBottomSheet = true
                        }
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AddTransactionForm(
                    viewModel = viewModel,
                    editingTransaction = editingTransaction, // Truyền vào để Form biết là đang Sửa hay Thêm
                    onSaved = { showBottomSheet = false }
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, viewModel: ExpenseViewModel, formatter: NumberFormat, onClick: () -> Unit) {
    val category = viewModel.categories.find { it.id == transaction.categoryId }
    val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(transaction.date)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")), CircleShape), contentAlignment = Alignment.Center) {
                Text(category?.name?.take(1) ?: "?", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(category?.name ?: "Khác", fontWeight = FontWeight.Bold)
                Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatter.format(transaction.amount), color = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336), fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.deleteTransaction(transaction.id) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun AddTransactionForm(viewModel: ExpenseViewModel, editingTransaction: Transaction?, onSaved: () -> Unit) {
    var amount by remember { mutableStateOf(if (editingTransaction != null) String.format(Locale.US, "%.0f", editingTransaction.amount) else "") }
    var note by remember { mutableStateOf(editingTransaction?.note ?: "") }
    var isIncome by remember { mutableStateOf(editingTransaction?.isIncome ?: false) }

    val availableCategories = viewModel.categories.filter { it.isIncome == isIncome }
    var selectedCategory by remember(isIncome, availableCategories) {
        mutableStateOf(availableCategories.find { it.id == editingTransaction?.categoryId } ?: availableCategories.firstOrNull())
    }

    // 🔥 XỬ LÝ CHỌN VÍ TIỀN
    val wallets = viewModel.wallets
    var selectedWallet by remember(wallets) {
        mutableStateOf(wallets.find { it.id == editingTransaction?.walletId } ?: wallets.firstOrNull())
    }

    val context = LocalContext.current
    var selectedDateMillis by remember { mutableStateOf(editingTransaction?.date ?: System.currentTimeMillis()) }
    val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDateMillis)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            selectedDateMillis = newCalendar.timeInMillis
        },
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.YEAR),
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.MONTH),
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.DAY_OF_MONTH)
    )

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (editingTransaction == null) "Thêm giao dịch" else "Sửa giao dịch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.padding(vertical = 16.dp)) {
            FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Chi tiêu") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2)))
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Thu nhập") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9)))
        }

        // --- CHỌN VÍ TIỀN (MỚI) ---
        Text("Nguồn tiền (Ví):", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(wallets) { wallet ->
                FilterChip(
                    selected = selectedWallet?.id == wallet.id,
                    onClick = { selectedWallet = wallet },
                    label = { Text(wallet.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = try { Color(android.graphics.Color.parseColor(wallet.colorHex)).copy(alpha = 0.5f) } catch (e: Exception) { Color.LightGray })
                )
            }
        }

        // --- CHỌN DANH MỤC ---
        Text("Danh mục:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start).padding(start = 4.dp))
        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(availableCategories) { cat ->
                FilterChip(
                    selected = selectedCategory?.id == cat.id,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat.name) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = try { Color(android.graphics.Color.parseColor(cat.colorHex)) } catch (e: Exception) { Color.LightGray })
                )
            }
        }

        OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) {
            Text("Ngày giao dịch: $dateString", color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Số tiền") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                if (parsedAmount > 0 && selectedCategory != null && selectedWallet != null) { // Bắt buộc phải có Ví
                    val transaction = Transaction(
                        id = editingTransaction?.id ?: "",
                        amount = parsedAmount,
                        note = note,
                        date = selectedDateMillis,
                        categoryId = selectedCategory!!.id,
                        isIncome = isIncome,
                        walletId = selectedWallet!!.id // Lưu ID ví vào
                    )
                    if (editingTransaction == null) viewModel.addTransaction(parsedAmount, note, selectedCategory!!.id, selectedWallet!!.id, isIncome, onSaved) // Sửa dòng này
                    else viewModel.updateTransaction(transaction, onSaved)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text(if (editingTransaction == null) "Lưu" else "Cập nhật")
        }
    }
}

@Composable
fun MonthNavigation(viewModel: ExpenseViewModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { viewModel.previousMonth() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
        Text("Tháng ${viewModel.selectedMonth + 1}, ${viewModel.selectedYear}", fontWeight = FontWeight.Bold)
        IconButton(onClick = { viewModel.nextMonth() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
    }
}