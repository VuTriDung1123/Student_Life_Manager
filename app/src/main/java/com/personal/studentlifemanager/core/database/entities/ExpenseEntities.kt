package com.personal.studentlifemanager.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isIncome: Boolean
)

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#4CAF50"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val note: String,
    val date: Long,
    val categoryId: String,
    val isIncome: Boolean,
    val walletId: String,
    val isTransfer: Boolean = false,
    val toWalletId: String = "",
    val currency: String = "VND"
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val amountLimit: Double,
    val month: Int,
    val year: Int
)

@Entity(tableName = "recurring_expenses")
data class RecurringExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val note: String,
    val categoryId: String,
    val walletId: String,
    val isIncome: Boolean,
    val nextExecutionTime: Long,
    val isActive: Boolean = true
)
