package com.personal.studentlifemanager.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.personal.studentlifemanager.data.dao.ExpenseDao
import com.personal.studentlifemanager.data.model.ExpenseEntity

@Database(entities = [ExpenseEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
}