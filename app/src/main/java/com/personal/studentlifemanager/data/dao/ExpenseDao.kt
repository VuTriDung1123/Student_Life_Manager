package com.personal.studentlifemanager.data.dao

import androidx.room.*
import com.personal.studentlifemanager.data.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    // Câu query "ăn tiền": Thống kê tổng tiền theo danh mục
    @Query("SELECT category, SUM(amount) as total FROM expenses GROUP BY category")
    fun getTotalByExpenes(): Flow<List<CategorySum>>
}

data class CategorySum(
    val category: String,
    val total: Double
)