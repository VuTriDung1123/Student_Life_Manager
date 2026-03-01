package com.personal.studentlifemanager.ui.screens

import android.app.DatePickerDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.app.TimePickerDialog
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import android.widget.DatePicker

@Composable
fun connectivityState(context: Context): State<Boolean> {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val isConnected = remember { mutableStateOf(connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) }
    DisposableEffect(context) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isConnected.value = true }
            override fun onLost(network: Network) { isConnected.value = false }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }
    return isConnected
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    onBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToCategory: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    viewModel: ExpenseViewModel = viewModel()
) {
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var quickAddType by remember { mutableIntStateOf(0) } // 0: Chi, 1: Thu, 2: Chuyển

    // 🔥 1. SNACKBAR STATE (CHO TÍNH NĂNG UNDO)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val transactions = viewModel.filteredTransactions
    val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    val context = LocalContext.current
    val isOnline by connectivityState(context)

    val displayMoney = { amount: Double -> if (viewModel.isBalanceHidden) "****** ₫" else formatter.format(amount) }

    val totalIncome = transactions.filter { it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val totalExpense = transactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }
    val balance = totalIncome - totalExpense

    // 🔥 2. XUẤT FILE CSV
    var csvContentToSave by remember { mutableStateOf("") }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { os ->
                OutputStreamWriter(os, "UTF-8").use { writer ->
                    writer.write("\uFEFF") // Thêm BOM để Excel đọc tiếng Việt không bị lỗi font
                    writer.write(csvContentToSave)
                }
            }
            Toast.makeText(context, "Đã lưu báo cáo CSV thành công!", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chi tiêu", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    // 🔥 1. NÚT CHỌN GIỜ NHẮC NHỞ MỚI THÊM
                    IconButton(onClick = {
                        val sharedPref = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
                        val currentHour = sharedPref.getInt("reminder_hour", 20)
                        val currentMinute = sharedPref.getInt("reminder_minute", 0)

                        TimePickerDialog(
                            context,
                            { _, selectedHour, selectedMinute ->
                                // Lưu giờ mới vào máy
                                sharedPref.edit()
                                    .putInt("reminder_hour", selectedHour)
                                    .putInt("reminder_minute", selectedMinute)
                                    .apply()

                                // Cập nhật lại WorkManager ngay lập tức
                                val currentDate = Calendar.getInstance()
                                val dueDate = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, selectedHour)
                                    set(Calendar.MINUTE, selectedMinute)
                                    set(Calendar.SECOND, 0)
                                }
                                if (dueDate.before(currentDate)) {
                                    dueDate.add(Calendar.HOUR_OF_DAY, 24)
                                }

                                val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis
                                val dailyWorkRequest = PeriodicWorkRequestBuilder<com.personal.studentlifemanager.worker.ReminderWorker>(24, TimeUnit.HOURS)
                                    .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
                                    .build()

                                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                    "DailyExpenseReminder",
                                    ExistingPeriodicWorkPolicy.UPDATE,
                                    dailyWorkRequest
                                )

                                Toast.makeText(context, "Đã hẹn giờ nhắc nhở hằng ngày lúc ${String.format("%02d:%02d", selectedHour, selectedMinute)}", Toast.LENGTH_SHORT).show()
                            },
                            currentHour, currentMinute, true // true để hiển thị định dạng 24h
                        ).show()
                    }) {
                        Icon(Icons.Default.NotificationsActive, "Hẹn giờ", tint = MaterialTheme.colorScheme.primary)
                    }

                    // 🔥 2. NÚT BẢO MẬT CON MẮT (Cũ)
                    IconButton(onClick = {
                        // Logic gọi vân tay của bạn giữ nguyên ở đây nhé
                        viewModel.toggleBalanceVisibility()
                    }) {
                        Icon(
                            imageVector = if (viewModel.isBalanceHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Bảo mật",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // 🔥 3. QUICK ADD (EXPANDING FAB)
            var fabExpanded by remember { mutableStateOf(false) }

            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Thu nhập", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(onClick = { fabExpanded = false; editingTransaction = null; quickAddType = 1; showBottomSheet = true }, containerColor = Color(0xFFC8E6C9)) { Icon(Icons.Default.ArrowDownward, "Thu", tint = Color(0xFF388E3C)) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Chuyển ví", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(onClick = { fabExpanded = false; editingTransaction = null; quickAddType = 2; showBottomSheet = true }, containerColor = Color(0xFFBBDEFB)) { Icon(Icons.Default.SyncAlt, "Chuyển", tint = Color(0xFF1976D2)) }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Chi tiêu", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            SmallFloatingActionButton(onClick = { fabExpanded = false; editingTransaction = null; quickAddType = 0; showBottomSheet = true }, containerColor = Color(0xFFFFCDD2)) { Icon(Icons.Default.ArrowUpward, "Chi", tint = Color(0xFFD32F2F)) }
                        }
                    }
                }
                FloatingActionButton(onClick = { fabExpanded = !fabExpanded }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(if (fabExpanded) Icons.Default.Close else Icons.Default.Add, null, tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AnimatedVisibility(visible = !isOnline, enter = expandVertically(), exit = shrinkVertically()) {
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFFF9C4)).padding(vertical = 8.dp, horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, contentDescription = "Offline", tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Đang Offline. Dữ liệu sẽ tự đồng bộ khi có mạng.", fontSize = 12.sp, color = Color(0xFFF57F17), fontWeight = FontWeight.Bold)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize()) {
                MonthNavigation(viewModel)
                // 🔥 THANH CÔNG CỤ NHANH (NẰM NGANG, VUỐT ĐƯỢC)
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AssistChip(
                            onClick = onNavigateToAnalytics,
                            label = { Text("Báo cáo") },
                            leadingIcon = { Icon(Icons.Default.Analytics, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onNavigateToBudget,
                            label = { Text("Ngân sách") },
                            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onNavigateToRecurring,
                            label = { Text("Định kỳ") },
                            leadingIcon = { Icon(Icons.Default.Autorenew, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onNavigateToCategory,
                            label = { Text("Danh mục") },
                            leadingIcon = { Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = {
                                // Logic xuất CSV giữ nguyên như cũ
                                val header = "Ngày,Loại,Danh mục,Nguồn tiền,Số tiền,Ghi chú\n"
                                val data = transactions.joinToString("\n") { t ->
                                    val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(t.date)
                                    val type = if (t.isTransfer) "Chuyển tiền" else if (t.isIncome) "Thu nhập" else "Chi tiêu"
                                    val cat = viewModel.categories.find { c -> c.id == t.categoryId }?.name ?: ""
                                    val wallet = viewModel.wallets.find { w -> w.id == t.walletId }?.name ?: ""
                                    val amt = t.amount.toLong().toString()
                                    val note = t.note.replace(",", " ")
                                    "$date,$type,$cat,$wallet,$amt,$note"
                                }
                                csvContentToSave = header + data
                                csvExportLauncher.launch("BaoCaoChiTieu_T${viewModel.selectedMonth + 1}.csv")
                            },
                            label = { Text("Xuất CSV") },
                            leadingIcon = { Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                Text("Số dư các ví", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(viewModel.wallets) { wallet ->
                        val walletBalance = viewModel.getWalletBalance(wallet.id)
                        Card(colors = CardDefaults.cardColors(containerColor = try { Color(android.graphics.Color.parseColor(wallet.colorHex)).copy(alpha = 0.2f) } catch(e:Exception){Color.LightGray})) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(wallet.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(displayMoney(walletBalance), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Số dư tháng này", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        Text(displayMoney(balance), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Thu nhập", style = MaterialTheme.typography.labelMedium)
                                Text(displayMoney(totalIncome), fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Chi tiêu", style = MaterialTheme.typography.labelMedium)
                                Text(displayMoney(totalExpense), fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                            }
                        }
                    }
                }

                Text("Lịch sử giao dịch", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = viewModel.searchQuery, onValueChange = { viewModel.searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(50.dp),
                    placeholder = { Text("Tìm kiếm ghi chú...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp), singleLine = true
                )

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(transactions, key = { it.id }) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            viewModel = viewModel,
                            displayMoney = displayMoney,
                            onClick = { editingTransaction = transaction; showBottomSheet = true },
                            onDelete = {
                                // 🔥 LOGIC XÓA CÓ UNDO
                                val deletedTx = transaction
                                viewModel.deleteTransaction(transaction.id)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Đã xóa giao dịch ${formatter.format(deletedTx.amount)}",
                                        actionLabel = "HOÀN TÁC",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        // Hoàn tác: Ghi lại dữ liệu cũ bằng hàm update
                                        viewModel.updateTransaction(deletedTx) {}
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
                AddTransactionForm(viewModel, editingTransaction, initialTxType = quickAddType) { showBottomSheet = false }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, viewModel: ExpenseViewModel, displayMoney: (Double) -> String, onClick: () -> Unit, onDelete: () -> Unit) {
    val dateString = SimpleDateFormat("dd/MM", Locale.getDefault()).format(transaction.date)

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (transaction.isTransfer) {
                val fromWallet = viewModel.wallets.find { it.id == transaction.walletId }?.name ?: "?"
                val toWallet = viewModel.wallets.find { it.id == transaction.toWalletId }?.name ?: "?"
                Box(modifier = Modifier.size(40.dp).background(Color(0xFF2196F3), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.SyncAlt, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Chuyển tiền", fontWeight = FontWeight.Bold)
                    Text("$fromWallet ➔ $toWallet", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(displayMoney(transaction.amount), color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
                }
            } else {
                val category = viewModel.categories.find { it.id == transaction.categoryId }
                Box(modifier = Modifier.size(40.dp).background(Color(android.graphics.Color.parseColor(category?.colorHex ?: "#CCCCCC")), CircleShape), contentAlignment = Alignment.Center) { Text(category?.name?.take(1) ?: "?", fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(category?.name ?: "Khác", fontWeight = FontWeight.Bold)
                    if (transaction.note.isNotEmpty()) Text(transaction.note, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val sign = if(transaction.isIncome) "+" else "-"
                    val amountText = if (displayMoney(transaction.amount) == "****** ₫") "****** ₫" else "$sign${displayMoney(transaction.amount)}"
                    Text(text = amountText, color = if (transaction.isIncome) Color(0xFF4CAF50) else Color(0xFFF44336), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, tint = Color.LightGray) }
                }
            }
        }
    }
}

@Composable
fun AddTransactionForm(viewModel: ExpenseViewModel, editingTransaction: Transaction?, initialTxType: Int, onSaved: () -> Unit) {
    var txType by remember { mutableIntStateOf(if (editingTransaction?.isTransfer == true) 2 else if (editingTransaction?.isIncome == true) 1 else if (editingTransaction != null) 0 else initialTxType) }
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

    var isProcessingAI by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            isProcessingAI = true
            viewModel.processReceiptImage(
                context = context, uri = it,
                onResult = { extractedAmount -> amount = extractedAmount; Toast.makeText(context, "Quét thành công: $extractedAmount", Toast.LENGTH_SHORT).show(); isProcessingAI = false },
                onError = { Toast.makeText(context, "Không nhận diện được số tiền!", Toast.LENGTH_SHORT).show(); isProcessingAI = false }
            )
        }
    }

    // Trạng thái hiển thị Popup cảnh báo
    var showAbnormalWarning by remember { mutableStateOf(false) }
    var pendingTransactionAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding() // Tránh dính thanh điều hướng ảo
            .imePadding() // 🔥 Tự động đẩy form lên khi bàn phím xuất hiện
            .verticalScroll(rememberScrollState()), // 🔥 Cho phép vuốt cuộn lên xuống
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (editingTransaction == null) "Giao dịch mới" else "Sửa giao dịch", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            FilterChip(selected = txType == 0, onClick = { txType = 0 }, label = { Text("Chi tiêu") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFFFCDD2)))
            FilterChip(selected = txType == 1, onClick = { txType = 1 }, label = { Text("Thu nhập") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFC8E6C9)))
            FilterChip(selected = txType == 2, onClick = { txType = 2 }, label = { Text("Chuyển ví") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFBBDEFB)))
        }

        if (txType == 2) {
            Text("Từ ví:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(wallets) { w -> FilterChip(selected = selectedWallet?.id == w.id, onClick = { selectedWallet = w }, label = { Text(w.name) }) } }
            Text("Đến ví:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(wallets) { w -> FilterChip(selected = selectedToWallet?.id == w.id, onClick = { selectedToWallet = w }, label = { Text(w.name) }) } }
        } else {
            Text("Nguồn tiền (Ví):", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(wallets) { w -> FilterChip(selected = selectedWallet?.id == w.id, onClick = { selectedWallet = w }, label = { Text(w.name) }) } }
            Text("Danh mục:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.align(Alignment.Start))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(availableCategories) { cat -> FilterChip(selected = selectedCategory?.id == cat.id, onClick = { selectedCategory = cat }, label = { Text(cat.name) }) } }
        }

        OutlinedButton(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth()) { Text("Ngày: $dateString", color = MaterialTheme.colorScheme.onSurface) }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Số tiền") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier.height(56.dp).padding(top = 8.dp), shape = RoundedCornerShape(8.dp), enabled = !isProcessingAI
            ) { if (isProcessingAI) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp)) else Icon(Icons.Default.DocumentScanner, "Quét Bill") }
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(if (txType == 2) "Lý do chuyển" else "Ghi chú") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                if (parsedAmount > 0) {
                    val saveAction = {
                        if (txType == 2 && selectedWallet != null && selectedToWallet != null && selectedWallet != selectedToWallet) {
                            val transaction = Transaction(editingTransaction?.id ?: "", parsedAmount, note, selectedDateMillis, "", false, selectedWallet!!.id, true, selectedToWallet!!.id)
                            if (editingTransaction == null) viewModel.addTransfer(parsedAmount, note, selectedWallet!!.id, selectedToWallet!!.id, selectedDateMillis, onSaved) else viewModel.updateTransaction(transaction, onSaved)
                        } else if (txType != 2 && selectedCategory != null && selectedWallet != null) {
                            val transaction = Transaction(editingTransaction?.id ?: "", parsedAmount, note, selectedDateMillis, selectedCategory!!.id, txType == 1, selectedWallet!!.id, false, "")
                            if (editingTransaction == null) viewModel.addTransaction(parsedAmount, note, selectedCategory!!.id, selectedWallet!!.id, txType == 1, onSaved) else viewModel.updateTransaction(transaction, onSaved)
                        }
                    }

                    if (txType == 0 && editingTransaction == null && viewModel.isAbnormalExpense(parsedAmount)) {
                        pendingTransactionAction = saveAction
                        showAbnormalWarning = true
                    } else {
                        saveAction()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text(if (editingTransaction == null) "Lưu" else "Cập nhật") }

        if (showAbnormalWarning) {
            AlertDialog(
                onDismissRequest = { showAbnormalWarning = false },
                title = { Text("Khoản chi bất thường!", fontWeight = FontWeight.Bold, color = Color(0xFFF44336)) },
                text = { Text("Khoản tiền này lớn hơn rất nhiều so với thói quen chi tiêu bình thường của bạn. Bạn có chắc chắn muốn lưu không?") },
                confirmButton = { Button(onClick = { showAbnormalWarning = false; pendingTransactionAction?.invoke() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))) { Text("Vẫn Lưu") } },
                dismissButton = { TextButton(onClick = { showAbnormalWarning = false }) { Text("Kiểm tra lại") } }
            )
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