package com.personal.studentlifemanager.data.model

// Data class cho Danh mục
data class Category(
    val id: String = "",
    val name: String = "",
    val iconName: String = "",
    val colorHex: String = "",
    val isIncome: Boolean = false
)

// Data class cho Giao dịch
data class Transaction(
    val id: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val date: Long = 0L,
    val categoryId: String = "",
    val isIncome: Boolean = false
)