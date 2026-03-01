package com.personal.studentlifemanager.data.model

import com.google.firebase.firestore.PropertyName

data class Category(
    var id: String = "",
    var name: String = "",
    var iconName: String = "",
    var colorHex: String = "",
    @get:PropertyName("isIncome")
    @set:PropertyName("isIncome")
    var isIncome: Boolean = false
)

data class Wallet(
    var id: String = "",
    var name: String = "",
    var colorHex: String = "#4CAF50"
)

data class Transaction(
    var id: String = "",
    var amount: Double = 0.0,
    var note: String = "",
    var date: Long = 0L,
    var categoryId: String = "",
    @get:PropertyName("isIncome")
    @set:PropertyName("isIncome")
    var isIncome: Boolean = false,
    var walletId: String = "",

    // 🔥 THÊM 2 TRƯỜNG NÀY ĐỂ XỬ LÝ CHUYỂN TIỀN
    @get:PropertyName("isTransfer")
    @set:PropertyName("isTransfer")
    var isTransfer: Boolean = false,
    var toWalletId: String = "" // ID của ví nhận tiền (nếu là giao dịch chuyển tiền)
)