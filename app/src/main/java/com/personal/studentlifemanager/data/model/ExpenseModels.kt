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

// --- THÊM CLASS NGÂN SÁCH MỚI ---
data class Budget(
    var id: String = "",
    var categoryId: String = "", // Ngân sách cho danh mục nào
    var amountLimit: Double = 0.0, // Giới hạn bao nhiêu tiền
    var month: Int = 0, // Áp dụng cho tháng nào
    var year: Int = 0 // Áp dụng cho năm nào
)

// --- THÊM CLASS GIAO DỊCH LẶP LẠI (TRẬN 4) ---
data class RecurringExpense(
    var id: String = "",
    var amount: Double = 0.0,
    var note: String = "",
    var categoryId: String = "",
    var walletId: String = "",
    @get:PropertyName("isIncome")
    @set:PropertyName("isIncome")
    var isIncome: Boolean = false,

    // Ngày tiếp theo sẽ tự động kích hoạt
    var nextExecutionTime: Long = 0L,

    // Cho phép người dùng Bật/Tắt chế độ tự động
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true
)