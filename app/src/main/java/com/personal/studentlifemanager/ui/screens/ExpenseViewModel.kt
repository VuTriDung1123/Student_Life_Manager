package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.personal.studentlifemanager.data.model.Category
import com.personal.studentlifemanager.data.model.Transaction
import com.personal.studentlifemanager.data.repository.ExpenseRepository

class ExpenseViewModel : ViewModel() {
    private val repository = ExpenseRepository()

    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set

    // DANH SÁCH DANH MỤC LẤY TỪ FIREBASE
    var categories by mutableStateOf<List<Category>>(emptyList())
        private set

    init {
        // Lắng nghe giao dịch
        repository.getTransactions { list -> transactions = list }

        // Lắng nghe danh mục
        repository.getCategories { list ->
            if (list.isEmpty()) {
                // Nếu user chưa có danh mục nào, tự động tạo các danh mục mặc định màu Pastel
                seedDefaultCategories()
            } else {
                categories = list
            }
        }
    }

    private fun seedDefaultCategories() {
        val defaults = listOf(
            Category("", "Ăn uống", "Fastfood", "#FFB6C1", false), // Hồng đào
            Category("", "Đi lại", "Commute", "#ADD8E6", false),  // Xanh dương nhạt
            Category("", "Học tập", "School", "#98FB98", false),  // Xanh lá nhạt
            Category("", "Trợ cấp/Lương", "Payments", "#FFD700", true) // Vàng
        )
        defaults.forEach { repository.addCategory(it) {} }
    }

    // -- CÁC HÀM GIAO DỊCH --
    fun addTransaction(amount: Double, note: String, categoryId: String, isIncome: Boolean, onSuccess: () -> Unit) {
        val transaction = Transaction("", amount, note, System.currentTimeMillis(), categoryId, isIncome)
        repository.addTransaction(transaction, onSuccess = { onSuccess() }, onFailure = {})
    }

    fun deleteTransaction(transactionId: String) {
        repository.deleteTransaction(transactionId, onSuccess = {}, onFailure = {})
    }

    // -- CÁC HÀM DANH MỤC --
    fun addCategory(name: String, colorHex: String, isIncome: Boolean, onSuccess: () -> Unit) {
        val newCat = Category("", name, "Default", colorHex, isIncome)
        repository.addCategory(newCat, onSuccess)
    }

    fun deleteCategory(categoryId: String) {
        repository.deleteCategory(categoryId)
    }
}