package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.personal.studentlifemanager.data.model.Budget
import com.personal.studentlifemanager.data.model.Category
import com.personal.studentlifemanager.data.model.RecurringExpense
import com.personal.studentlifemanager.data.model.Transaction
import com.personal.studentlifemanager.data.model.Wallet
import com.personal.studentlifemanager.data.repository.ExpenseRepository
import java.util.Calendar
import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.lifecycle.viewModelScope

class ExpenseViewModel : ViewModel() {
    private val repository = ExpenseRepository()

    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set
    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var wallets by mutableStateOf<List<Wallet>>(emptyList())
        private set
    var budgets by mutableStateOf<List<Budget>>(emptyList())
        private set
    var recurringExpenses by mutableStateOf<List<RecurringExpense>>(emptyList())
        private set
    var isBalanceHidden by mutableStateOf(true)
        private set

    var selectedMonth by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var selectedYear by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))
    var searchQuery by mutableStateOf("")
    val supportedCurrencies = listOf("VND", "USD", "EUR", "JPY", "KRW")
    var exchangeRates by mutableStateOf<Map<String, Double>>(emptyMap())
        private set


    // Lọc giao dịch không phải chuyển tiền để tính Thống kê Thu/Chi chuẩn xác
    val filteredTransactions: List<Transaction>
        get() = transactions.filter { t ->
            val cal = Calendar.getInstance().apply { timeInMillis = t.date }
            val matchesDate = cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
            val matchesSearch = t.note.contains(searchQuery, ignoreCase = true)
            matchesDate && matchesSearch
        }.sortedByDescending { it.date }

    val allTransactions: List<Transaction> get() = transactions

    init {
        repository.getTransactions { transactions = it }
        repository.getCategories { list -> if (list.isEmpty()) seedDefaultCategories() else categories = list }
        repository.getWallets { list -> if (list.isEmpty()) seedDefaultWallets() else wallets = list }
        repository.getBudgets { list -> budgets = list }

        repository.getRecurringExpenses { list ->
            recurringExpenses = list
            checkAndExecuteRecurring(list) // Gọi thuật toán mỗi khi có data
        }
        fetchExchangeRates()
    }

    // 🔥 HÀM TÍNH SỐ DƯ TỪNG VÍ (Tuyệt chiêu xử lý dòng tiền)
    fun getWalletBalance(walletId: String): Double {
        // Thu nhập vào ví
        val incomes = allTransactions.filter { it.walletId == walletId && it.isIncome && !it.isTransfer }.sumOf { it.amount }
        // Chi tiêu từ ví
        val expenses = allTransactions.filter { it.walletId == walletId && !it.isIncome && !it.isTransfer }.sumOf { it.amount }
        // Chuyển tiền ĐI từ ví này
        val transferOut = allTransactions.filter { it.walletId == walletId && it.isTransfer }.sumOf { it.amount }
        // Chuyển tiền ĐẾN ví này
        val transferIn = allTransactions.filter { it.toWalletId == walletId && it.isTransfer }.sumOf { it.amount }

        return incomes - expenses - transferOut + transferIn
    }

    private fun seedDefaultCategories() {
        val defaults = listOf(Category("", "Ăn uống", "", "#FFB6C1", false), Category("", "Trợ cấp", "", "#FFD700", true))
        defaults.forEach { repository.addCategory(it) {} }
    }

    private fun seedDefaultWallets() {
        val defaults = listOf(Wallet("", "Tiền mặt", "#4CAF50"), Wallet("", "Tài khoản NH", "#2196F3"))
        defaults.forEach { repository.addWallet(it) {} }
    }

    // 🔥 CẬP NHẬT 2 HÀM NÀY ĐỂ NHẬN LOẠI TIỀN (CURRENCY)
    fun addTransaction(amount: Double, note: String, categoryId: String, walletId: String, isIncome: Boolean, currency: String = "VND", onSuccess: () -> Unit) {
        val transaction = Transaction("", amount, note, System.currentTimeMillis(), categoryId, isIncome, walletId, false, "", currency)
        repository.addTransaction(transaction, onSuccess, {})
    }

    fun addTransfer(amount: Double, note: String, fromWalletId: String, toWalletId: String, dateMillis: Long, currency: String = "VND", onSuccess: () -> Unit) {
        val transaction = Transaction("", amount, note, dateMillis, "", false, fromWalletId, true, toWalletId, currency)
        repository.addTransaction(transaction, onSuccess, {})
    }

    fun updateTransaction(transaction: Transaction, onSuccess: () -> Unit) = repository.updateTransaction(transaction, onSuccess)
    fun deleteTransaction(transactionId: String) = repository.deleteTransaction(transactionId, {}, {})

    fun previousMonth() { if (selectedMonth == 0) { selectedMonth = 11; selectedYear -= 1 } else selectedMonth -= 1 }
    fun nextMonth() { if (selectedMonth == 11) { selectedMonth = 0; selectedYear += 1 } else selectedMonth += 1 }

    fun addCategory(name: String, colorHex: String, isIncome: Boolean, onSuccess: () -> Unit) {
        repository.addCategory(Category("", name, "Default", colorHex, isIncome), onSuccess)
    }
    fun deleteCategory(categoryId: String) = repository.deleteCategory(categoryId)

    // --- HÀM XỬ LÝ NGÂN SÁCH ---
    fun saveBudget(categoryId: String, limit: Double, onSuccess: () -> Unit) {
        // Tìm xem tháng này đã có ngân sách cho danh mục này chưa để ghi đè (sửa) hoặc tạo mới
        val existing = budgets.find { it.categoryId == categoryId && it.month == selectedMonth && it.year == selectedYear }
        val budget = Budget(
            id = existing?.id ?: "",
            categoryId = categoryId,
            amountLimit = limit,
            month = selectedMonth,
            year = selectedYear
        )
        repository.saveBudget(budget, onSuccess)
    }

    fun deleteBudget(budgetId: String) = repository.deleteBudget(budgetId)

    // --- HÀM TÍNH XU HƯỚNG BÁO CÁO (TRẬN 3) ---
    fun getExpenseForMonth(month: Int, year: Int): Double {
        return allTransactions.filter { t ->
            val cal = Calendar.getInstance().apply { timeInMillis = t.date }
            cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year && !t.isIncome && !t.isTransfer
        }.sumOf { it.amount }
    }
    // --- THUẬT TOÁN KIỂM TRA & TỰ ĐỘNG CHẠY GIAO DỊCH ---
    private fun checkAndExecuteRecurring(list: List<RecurringExpense>) {
        val now = System.currentTimeMillis()

        // Lọc ra các mục đang Bật (isActive) và đã đến hạn (nextExecutionTime <= now)
        val dueItems = list.filter { it.isActive && it.nextExecutionTime <= now }

        dueItems.forEach { rec ->
            // 1. Tự động tạo một Giao dịch thật sự vào lịch sử
            val newTransaction = Transaction(
                id = "",
                amount = rec.amount,
                note = "${rec.note} (Tự động)", // Thêm chữ để user biết app tự trừ
                date = rec.nextExecutionTime,
                categoryId = rec.categoryId,
                isIncome = rec.isIncome,
                walletId = rec.walletId
            )
            repository.addTransaction(newTransaction, {}, {})

            // 2. Tính toán ngày tháng tiếp theo (+1 tháng)
            val cal = Calendar.getInstance().apply { timeInMillis = rec.nextExecutionTime }
            cal.add(Calendar.MONTH, 1)

            // 3. Cập nhật lại lịch lặp lại lên Firebase
            rec.nextExecutionTime = cal.timeInMillis
            repository.saveRecurring(rec, {})
        }
    }

    // --- HÀM CHO UI GỌI ---
    fun saveRecurring(recurring: RecurringExpense, onSuccess: () -> Unit) = repository.saveRecurring(recurring, onSuccess)
    fun deleteRecurring(recurringId: String) = repository.deleteRecurring(recurringId)
    fun toggleRecurringState(recurring: RecurringExpense) {
        recurring.isActive = !recurring.isActive
        repository.saveRecurring(recurring, {})
    }

    fun toggleBalanceVisibility() {
        isBalanceHidden = !isBalanceHidden
    }
    // ==========================================
    // --- KHU VỰC 6: AI OCR QUÉT HÓA ĐƠN ---
    // ==========================================
    fun processReceiptImage(context: Context, uri: Uri, onResult: (String) -> Unit, onError: () -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    // Thuật toán: Lọc tìm con số lớn nhất
                    val extractedAmount = extractMaxAmount(text)
                    if (extractedAmount > 0) {
                        onResult(extractedAmount.toLong().toString())
                    } else {
                        onError()
                    }
                }
                .addOnFailureListener {
                    onError()
                }
        } catch (e: Exception) {
            onError()
        }
    }

    private fun extractMaxAmount(text: String): Double {
        // 1. Tìm các số có dấu phân cách (VD: 88.000, 1.500.000)
        // 2. Hoặc các số viết liền từ 4 đến 9 chữ số (VD: 88000)
        val regex = Regex("""\b[1-9]\d{0,2}(?:[.,]\d{3})+\b|\b[1-9]\d{3,8}\b""")
        val matches = regex.findAll(text)

        var maxAmount = 0.0
        for (match in matches) {
            // Gỡ bỏ dấu chấm, dấu phẩy để biến thành số thực tế
            val numStr = match.value.replace(",", "").replace(".", "")
            val num = numStr.toDoubleOrNull() ?: 0.0

            // 🔥 THUẬT TOÁN LỌC VÀNG CHO TIỀN VNĐ
            // 1. num > 1000: Hóa đơn ít khi nào dưới 1k
            // 2. num < 1,000,000,000: Dưới 1 tỷ để tránh nhầm mã vạch/ID dài ngoằng
            // 3. num % 100 == 0.0: TIỀN VNĐ PHẢI CHIA HẾT CHO 100 (Loại bỏ ngay 98.198 hay 12.181)
            if (num > 1000 && num < 1000000000 && num % 100 == 0.0) {
                if (num > maxAmount) {
                    maxAmount = num
                }
            }
        }
        return maxAmount
    }

    // ==========================================
    // --- KHU VỰC 7: AI PHÂN TÍCH & DỰ ĐOÁN ---
    // ==========================================

    // Thuật toán 1: Phát hiện chi tiêu bất thường
    fun isAbnormalExpense(amount: Double): Boolean {
        val expenseList = allTransactions.filter { !it.isIncome && !it.isTransfer }
        if (expenseList.isEmpty()) return false

        // Tính trung bình 1 lần chi tiêu của bạn
        val avgExpense = expenseList.sumOf { it.amount } / expenseList.size

        // Bất thường = Lớn hơn 3 lần mức trung bình VÀ phải lớn hơn 500k (bỏ qua mấy khoản lặt vặt)
        return amount > (avgExpense * 3) && amount > 500000.0
    }

    // Thuật toán 2: Dự đoán tổng chi tiêu cuối tháng dựa trên tốc độ tiêu tiền hiện tại
    fun getPredictedEndOfMonthExpense(): Double {
        val currentMonthExpenses = filteredTransactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }

        val cal = Calendar.getInstance()
        val todayMonth = cal.get(Calendar.MONTH)
        val todayYear = cal.get(Calendar.YEAR)

        // Chỉ dự đoán cho tháng hiện tại đang diễn ra
        if (selectedMonth == todayMonth && selectedYear == todayYear) {
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            if (currentDay == 0) return currentMonthExpenses

            // Tốc độ đốt tiền 1 ngày
            val dailyBurnRate = currentMonthExpenses / currentDay
            // Dự đoán cả tháng
            return dailyBurnRate * maxDays
        }
        return currentMonthExpenses // Nếu xem tháng cũ thì trả về số thực tế
    }

    // ==========================================
    // --- KHU VỰC 8: XỬ LÝ ĐA TIỀN TỆ (MULTI-CURRENCY) ---
    // ==========================================

    // 1. Gọi API Lấy tỷ giá Real-time (Lấy VND làm gốc)
    private fun fetchExchangeRates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://open.er-api.com/v6/latest/VND")
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val rates = json.getJSONObject("rates")

                val map = mutableMapOf<String, Double>()
                supportedCurrencies.forEach { curr ->
                    if (rates.has(curr)) map[curr] = rates.getDouble(curr)
                }
                withContext(Dispatchers.Main) { exchangeRates = map }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // 2. Tính tiền gom theo từng loại trong 1 ví (Map<Tiền, Số dư>)
    fun getWalletBalancesMulti(walletId: String): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()
        allTransactions.filter { it.walletId == walletId }.forEach { t ->
            val current = balances[t.currency] ?: 0.0
            if (t.isTransfer) {
                if (t.walletId == walletId) balances[t.currency] = current - t.amount
            } else {
                balances[t.currency] = if (t.isIncome) current + t.amount else current - t.amount
            }
        }
        allTransactions.filter { it.toWalletId == walletId && it.isTransfer }.forEach { t ->
            balances[t.currency] = (balances[t.currency] ?: 0.0) + t.amount
        }
        // Xóa các loại tiền đang có số dư = 0 cho đỡ rác
        return balances.filterValues { it != 0.0 }
    }

    // 3. Hàm quy đổi mọi thứ ra VND để tham khảo
    fun convertToVND(amount: Double, currency: String): Double {
        if (currency == "VND") return amount
        val rate = exchangeRates[currency] ?: return 0.0
        return amount / rate // Ví dụ: USD = 0.00004 -> 100 USD / 0.00004 = 2.500.000 VND
    }
}