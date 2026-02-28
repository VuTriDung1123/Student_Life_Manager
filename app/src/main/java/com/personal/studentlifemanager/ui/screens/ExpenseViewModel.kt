package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.personal.studentlifemanager.data.model.Category
import com.personal.studentlifemanager.data.model.Transaction
import com.personal.studentlifemanager.data.repository.ExpenseRepository
import java.util.Calendar

class ExpenseViewModel : ViewModel() {
    private val repository = ExpenseRepository()

    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set

    var categories by mutableStateOf<List<Category>>(emptyList())
        private set

    // --- 🕒 THÊM BỘ LỌC THÁNG / NĂM ---
    var selectedMonth by mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) // 0 đến 11
    var selectedYear by mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR))

    // Dữ liệu "thực" sẽ đưa ra màn hình (Chỉ lấy giao dịch thuộc tháng/năm đang chọn)
    val filteredTransactions: List<Transaction>
        get() = transactions.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear
        }

    // Hàm lùi tháng
    fun previousMonth() {
        if (selectedMonth == 0) {
            selectedMonth = 11
            selectedYear -= 1
        } else {
            selectedMonth -= 1
        }
    }

    // Hàm tiến tháng
    fun nextMonth() {
        if (selectedMonth == 11) {
            selectedMonth = 0
            selectedYear += 1
        } else {
            selectedMonth += 1
        }
    }
    // ------------------------------------

    init {
        repository.getTransactions { list -> transactions = list }
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

    fun addTransaction(amount: Double, note: String, categoryId: String, isIncome: Boolean, onSuccess: () -> Unit) {
        val transaction = Transaction("", amount, note, System.currentTimeMillis(), categoryId, isIncome)
        repository.addTransaction(transaction, onSuccess = { onSuccess() }, onFailure = {})
    }

    fun deleteTransaction(transactionId: String) {
        repository.deleteTransaction(transactionId, onSuccess = {}, onFailure = {})
    }

    fun addCategory(name: String, colorHex: String, isIncome: Boolean, onSuccess: () -> Unit) {
        val newCat = Category("", name, "Default", colorHex, isIncome)
        repository.addCategory(newCat, onSuccess)
    }

    fun deleteCategory(categoryId: String) {
        repository.deleteCategory(categoryId)
    }
}