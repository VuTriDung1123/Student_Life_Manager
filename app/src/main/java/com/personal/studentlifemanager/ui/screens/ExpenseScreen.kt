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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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
    onNavigateToBudget: () -> Unit,
    navController: NavController = rememberNavController(),
    viewModel: ExpenseViewModel = viewModel()
) {
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val transactions = viewModel.filteredTransactions
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

    // Chỉ tính Thu/Chi thực tế, bỏ qua Chuyển tiền
    val totalIncome = transactions.filter { it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý chi tiêu", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    // Nút Ngân sách đã được sửa onClick
                    IconButton(onClick = onNavigateToBudget) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNavigateToCategory) { Icon(Icons.Default.List, null, tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onNavigateToAnalytics) { Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingTransaction = null
                showBottomSheet = true
            }) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 16.dp).fillMaxSize()) {

            // 1. THANH CHUYỂN THÁNG
            MonthNavigation(viewModel)

            // 2. DANH SÁCH SỐ DƯ TỪNG VÍ (Nằm ngang)
            Text("Số dư các ví", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.wallets) { wallet ->
                    val walletBalance = viewModel.getWalletBalance(wallet.id)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = try { Color(android.graphics.Color.parseColor(wallet.colorHex)).copy(alpha = 0.2f) } catch(e:Exception){Color.LightGray})
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(wallet.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(formatter.format(walletBalance), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 3. TỔNG QUAN THÁNG
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Số dư tháng này", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text(formatter.format(balance), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
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

            // 4. LỊCH SỬ GIAO DỊCH
            Text("Lịch sử giao dịch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = viewModel.searchQuery, onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(50.dp),
                placeholder = { Text("Tìm kiếm ghi chú...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp), singleLine = true
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(transactions) { transaction ->
                    TransactionItem(transaction, viewModel, formatter) {
                        editingTransaction = transaction
                        showBottomSheet = true
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                AddTransactionForm(viewModel, editingTransaction) { showBottomSheet = false }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, viewModel: ExpenseViewModel, formatter: NumberFormat, onClick: () -> Unit) {
    val dateString = SimpleDateFormat("dd/MM", Locale.getDefault()).format(transaction.date)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // PHÂN BIỆT RÕ GIAO DỊCH THƯỜNG VÀ CHUYỂN TIỀN
            if (transaction.isTransfer) {
                val fromWallet = viewModel.wallets.find { it.id == transaction.walletId }?.name ?: "?"
                val toWallet = viewModel.wallets.find { it.id == transaction.toWalletId }?.name ?: "?"
                Box(modifier = Modifier.size(40.dp).background(Color(0xFF2196F3), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chuyển tiền", fontWeight = FontWeight.Bold)
                    Text("$fromWallet ➔ $toWallet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatter.format(transaction.amount), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.deleteTransaction(transaction.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
                }
            } else {
                val category = viewModel.categories.find { it.id == transaction.categoryId }
                Box(modifier = Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")), CircleShape), contentAlignment = Alignment.Center) {
                    Text(category?.name?.take(1) ?: "?", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category?.name ?: "Khác", fontWeight = FontWeight.Bold)
                    if (transaction.note.isNotEmpty()) Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if(transaction.isIncome) "+" else "-"}${formatter.format(transaction.amount)}",
                        color = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336), fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.deleteTransaction(transaction.id) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
                }
            }
        }
    }
}

@Composable
fun AddTransactionForm(viewModel: ExpenseViewModel, editingTransaction: Transaction?, onSaved: () -> Unit) {
    // txType: 0=Chi, 1=Thu, 2=Chuyển tiền
    var txType by remember { mutableIntStateOf(if (editingTransaction?.isTransfer == true) 2 else if (editingTransaction?.isIncome == true) 1 else 0) }
    var amount by remember { mutableStateOf(if (editingTransaction != null) String.format(Locale.US, "%.0f", editingTransaction.amount) else "") }
    var note by remember { mutableStateOf(editingTransaction?.note ?: "") }

    val availableCategories = viewModel.categories.filter { it.isIncome == (txType == 1) }
    var selectedCategory by remember(txType) { mutableStateOf(availableCategories.find { it.id == editingTransaction?.categoryId } ?: availableCategories.firstOrNull()) }

    val wallets = viewModel.wallets
    var selectedWallet by remember { mutableStateOf(wallets.find { it.id == editingTransaction?.walletId } ?: wallets.firstOrNull()) }
    var selectedToWallet by remember { mutableStateOf(wallets.find { it.id == editingTransaction?.toWalletId } ?: wallets.lastOrNull()) }

    val context = LocalContext.current
    var selectedDateMillis by remember { mutableStateOf(editingTransaction?.date ?: System.currentTimeMillis()) }
    val dateString = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(selectedDateMillis)

    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> val cal = Calendar.getInstance(); cal.set(y,m,d); selectedDateMillis = cal.timeInMillis }, Calendar.getInstance().get(Calendar.YEAR), Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.DAY_OF_MONTH))

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (editingTransaction == null) "Giao dịch mới" else "Sửa giao dịch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(selected = txType == 0, onClick = { txType = 0 }, label = { Text("Chi tiêu") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2)))
            FilterChip(selected = txType == 1, onClick = { txType = 1 }, label = { Text("Thu nhập") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9)))
            FilterChip(selected = txType == 2, onClick = { txType = 2 }, label = { Text("Chuyển ví") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFBBDEFB)))
        }

        if (txType == 2) {
            Text("Từ ví:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wallets) { w -> FilterChip(selected = selectedWallet?.id == w.id, onClick = { selectedWallet = w }, label = { Text(w.name) }) }
            }
            Text("Đến ví:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wallets) { w -> FilterChip(selected = selectedToWallet?.id == w.id, onClick = { selectedToWallet = w }, label = { Text(w.name) }) }
            }
        } else {
            Text("Nguồn tiền (Ví):", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(wallets) { w -> FilterChip(selected = selectedWallet?.id == w.id, onClick = { selectedWallet = w }, label = { Text(w.name) }) }
            }
            Text("Danh mục:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableCategories) { cat -> FilterChip(selected = selectedCategory?.id == cat.id, onClick = { selectedCategory = cat }, label = { Text(cat.name) }) }
            }
        }

        OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) { Text("Ngày: $dateString", color = MaterialTheme.colorScheme.onSurface) }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Số tiền") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(if (txType == 2) "Lý do chuyển" else "Ghi chú") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                if (parsedAmount > 0) {
                    if (txType == 2 && selectedWallet != null && selectedToWallet != null && selectedWallet != selectedToWallet) {
                        val transaction = Transaction(editingTransaction?.id ?: "", parsedAmount, note, selectedDateMillis, "", false, selectedWallet!!.id, true, selectedToWallet!!.id)
                        if (editingTransaction == null) viewModel.addTransfer(parsedAmount, note, selectedWallet!!.id, selectedToWallet!!.id, selectedDateMillis, onSaved) else viewModel.updateTransaction(transaction, onSaved)
                    } else if (txType != 2 && selectedCategory != null && selectedWallet != null) {
                        val transaction = Transaction(editingTransaction?.id ?: "", parsedAmount, note, selectedDateMillis, selectedCategory!!.id, txType == 1, selectedWallet!!.id, false, "")
                        if (editingTransaction == null) viewModel.addTransaction(parsedAmount, note, selectedCategory!!.id, selectedWallet!!.id, txType == 1, onSaved) else viewModel.updateTransaction(transaction, onSaved)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text(if (editingTransaction == null) "Lưu" else "Cập nhật") }
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