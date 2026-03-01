package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.personal.studentlifemanager.data.model.Category
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

    // --- 🔍 TÌM KIẾM & BỘ LỌC ---
    var selectedMonth by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var selectedYear by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))
    var searchQuery by mutableStateOf("") // Lưu từ khóa tìm kiếm

    // Danh sách "xịn" đã qua lọc và tìm kiếm để đưa ra màn hình
    val filteredTransactions: List<Transaction>
        get() = transactions.filter { t ->
            val cal = Calendar.getInstance().apply { timeInMillis = t.date }
            val matchesDate = cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
            val matchesSearch = t.note.contains(searchQuery, ignoreCase = true)
            matchesDate && matchesSearch
        }.sortedByDescending { it.date } // Luôn để cái mới nhất lên đầu

    // Để trang Analytics lấy hết data
    val allTransactions: List<Transaction> get() = transactions

    init {
        repository.getTransactions { transactions = it }
        repository.getCategories { list ->
            if (list.isEmpty()) seedDefaultCategories() else categories = list
        }
    }

    private fun seedDefaultCategories() {
        val defaults = listOf(
            Category("", "Ăn uống", "Fastfood", "#FFB6C1", false),
            Category("", "Đi lại", "Commute", "#ADD8E6", false),
            Category("", "Học tập", "School", "#98FB98", false),
            Category("", "Trợ cấp/Lương", "Payments", "#FFD700", true)
        )
        defaults.forEach { repository.addCategory(it) {} }
    }

    // 🔥 THÊM BIẾN LƯU VÍ
    var wallets by mutableStateOf<List<Wallet>>(emptyList())
        private set

    init {
        repository.getTransactions { transactions = it }
        repository.getCategories { list ->
            if (list.isEmpty()) seedDefaultCategories() else categories = list
        }
        // 🔥 LẮNG NGHE DỮ LIỆU VÍ TỪ FIREBASE
        repository.getWallets { list ->
            if (list.isEmpty()) seedDefaultWallets() else wallets = list
        }
    }

    // Tự động tạo ví mặc định cho tài khoản mới
    private fun seedDefaultWallets() {
        val defaults = listOf(
            Wallet("", "Tiền mặt", "#4CAF50"),      // Xanh lá
            Wallet("", "Tài khoản NH / Momo", "#2196F3") // Xanh dương
        )
        defaults.forEach { repository.addWallet(it) {} }
    }

    // --- CÁC HÀM XỬ LÝ DỮ LIỆU ---
    fun addTransaction(amount: Double, note: String, categoryId: String, walletId: String, isIncome: Boolean, onSuccess: () -> Unit) {
        val transaction = Transaction("", amount, note, System.currentTimeMillis(), categoryId, isIncome, walletId)
        repository.addTransaction(transaction, onSuccess = { onSuccess() }, onFailure = {})
    }

    fun updateTransaction(transaction: Transaction, onSuccess: () -> Unit) {
        repository.updateTransaction(transaction, onSuccess)
    }

    fun deleteTransaction(transactionId: String) {
        repository.deleteTransaction(transactionId, {}, {})
    }

    fun previousMonth() { /* Code cũ của bạn */
        if (selectedMonth == 0) { selectedMonth = 11; selectedYear -= 1 } else selectedMonth -= 1
    }
    fun nextMonth() { /* Code cũ của bạn */
        if (selectedMonth == 11) { selectedMonth = 0; selectedYear += 1 } else selectedMonth += 1
    }

    fun addCategory(name: String, colorHex: String, isIncome: Boolean, onSuccess: () -> Unit) {
        repository.addCategory(Category("", name, "Default", colorHex, isIncome), onSuccess)
    }
    fun deleteCategory(categoryId: String) = repository.deleteCategory(categoryId)
}