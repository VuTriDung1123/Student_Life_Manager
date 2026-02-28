package com.personal.studentlifemanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,          // Tên khoản chi (ví dụ: Ăn trưa)
    val amount: Double,         // Số tiền
    val category: String,       // Danh mục (Ăn uống, Tiền trọ, Học phí...)
    val date: Long,             // Ngày chi (Lưu dạng Long/Timestamp cho dễ sắp xếp)
    val note: String = ""       // Ghi chú thêm
)