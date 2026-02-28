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

    // 🌸 Bơm sẵn vài danh mục mặc định tông màu Pastel
    val defaultCategories = listOf(
        Category("cat_1", "Ăn uống", "Fastfood", "#FFB6C1", false),
        Category("cat_2", "Đi lại", "Commute", "#ADD8E6", false),
        Category("cat_3", "Học tập", "School", "#98FB98", false),
        Category("cat_4", "Trợ cấp/Lương", "Payments", "#FFD700", true)
    )

    // THÊM CÁI NÀY: Biến chứa danh sách giao dịch để màn hình hiển thị
    var transactions by mutableStateOf<List<Transaction>>(emptyList())
        private set

    init {
        // Tự động lắng nghe dữ liệu từ Firebase ngay khi mở app
        repository.getTransactions { list ->
            transactions = list
        }
    }

    fun addTransaction(amount: Double, note: String, categoryId: String, isIncome: Boolean, onSuccess: () -> Unit) {
        val transaction = Transaction(
            amount = amount,
            note = note,
            date = System.currentTimeMillis(),
            categoryId = categoryId,
            isIncome = isIncome
        )

        repository.addTransaction(
            transaction = transaction,
            onSuccess = { onSuccess() },
            onFailure = { e -> println("Lỗi lưu giao dịch: ${e.message}") }
        )
    }
}