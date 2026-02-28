package com.personal.studentlifemanager.data.model

import com.google.firebase.firestore.PropertyName

data class Category(
    var id: String = "",
    var name: String = "",
    var iconName: String = "",
    var colorHex: String = "",
    // Ép Firebase phải lưu và đọc đúng chữ "isIncome"
    @get:PropertyName("isIncome")
    @set:PropertyName("isIncome")
    var isIncome: Boolean = false
)

data class Transaction(
    var id: String = "",
    var amount: Double = 0.0,
    var note: String = "",
    var date: Long = 0L,
    var categoryId: String = "",
    // Ép Firebase phải lưu và đọc đúng chữ "isIncome"
    @get:PropertyName("isIncome")
    @set:PropertyName("isIncome")
    var isIncome: Boolean = false
)