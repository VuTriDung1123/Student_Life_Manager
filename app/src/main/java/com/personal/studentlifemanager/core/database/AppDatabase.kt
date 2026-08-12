package com.personal.studentlifemanager.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.personal.studentlifemanager.core.database.dao.ExpenseDao
import com.personal.studentlifemanager.core.database.dao.FlashcardDao
import com.personal.studentlifemanager.core.database.dao.HabitDao
import com.personal.studentlifemanager.core.database.dao.UserProfileDao
import com.personal.studentlifemanager.core.database.entities.BudgetEntity
import com.personal.studentlifemanager.core.database.entities.CategoryEntity
import com.personal.studentlifemanager.core.database.entities.DeckEntity
import com.personal.studentlifemanager.core.database.entities.FlashcardActivityEntity
import com.personal.studentlifemanager.core.database.entities.FlashcardEntity
import com.personal.studentlifemanager.core.database.entities.HabitEntity
import com.personal.studentlifemanager.core.database.entities.HabitLogEntity
import com.personal.studentlifemanager.core.database.entities.RecurringExpenseEntity
import com.personal.studentlifemanager.core.database.entities.TransactionEntity
import com.personal.studentlifemanager.core.database.entities.UserProfileEntity
import com.personal.studentlifemanager.core.database.entities.WalletEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        WalletEntity::class,
        BudgetEntity::class,
        RecurringExpenseEntity::class,
        DeckEntity::class,
        FlashcardEntity::class,
        FlashcardActivityEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun habitDao(): HabitDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "student_life_manager_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
