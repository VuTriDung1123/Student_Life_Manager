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

    fun addTransaction(amount: Double, note: String, categoryId: String, walletId: String, isIncome: Boolean, onSuccess: () -> Unit) {
        val transaction = Transaction("", amount, note, System.currentTimeMillis(), categoryId, isIncome, walletId)
        repository.addTransaction(transaction, onSuccess, {})
    }

    // 🔥 HÀM THÊM LỆNH CHUYỂN TIỀN
    fun addTransfer(amount: Double, note: String, fromWalletId: String, toWalletId: String, dateMillis: Long, onSuccess: () -> Unit) {
        val transaction = Transaction("", amount, note, dateMillis, "", false, fromWalletId, true, toWalletId)
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
}