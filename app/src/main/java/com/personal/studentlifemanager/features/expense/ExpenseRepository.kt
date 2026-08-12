package com.personal.studentlifemanager.features.expense

import com.personal.studentlifemanager.core.database.dao.ExpenseDao
import com.personal.studentlifemanager.core.database.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    // ==========================================
    // --- TRANSACTIONS ---
    // ==========================================
    fun getTransactions(): Flow<List<Transaction>> {
        return expenseDao.getAllTransactions().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addTransaction(transaction: Transaction) {
        val entity = transaction.toEntity().copy(
            id = if (transaction.id.isEmpty()) UUID.randomUUID().toString() else transaction.id
        )
        expenseDao.insertTransaction(entity)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        expenseDao.updateTransaction(transaction.toEntity())
    }

    suspend fun deleteTransaction(transactionId: String) {
        expenseDao.deleteTransaction(TransactionEntity(id = transactionId, amount = 0.0, note = "", date = 0, categoryId = "", isIncome = false, walletId = ""))
    }

    // ==========================================
    // --- CATEGORIES ---
    // ==========================================
    fun getCategories(): Flow<List<Category>> {
        return expenseDao.getAllCategories().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addCategory(category: Category) {
        val entity = category.toEntity().copy(
            id = if (category.id.isEmpty()) UUID.randomUUID().toString() else category.id
        )
        expenseDao.insertCategory(entity)
    }

    suspend fun deleteCategory(categoryId: String) {
        expenseDao.deleteCategory(CategoryEntity(id = categoryId, name = "", iconName = "", colorHex = "", isIncome = false))
    }

    // ==========================================
    // --- WALLETS ---
    // ==========================================
    fun getWallets(): Flow<List<Wallet>> {
        return expenseDao.getAllWallets().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addWallet(wallet: Wallet) {
        val entity = wallet.toEntity().copy(
            id = if (wallet.id.isEmpty()) UUID.randomUUID().toString() else wallet.id
        )
        expenseDao.insertWallet(entity)
    }

    suspend fun deleteWallet(walletId: String) {
        expenseDao.deleteWallet(WalletEntity(id = walletId, name = ""))
    }

    // ==========================================
    // --- BUDGETS ---
    // ==========================================
    fun getBudgets(): Flow<List<Budget>> {
        return expenseDao.getAllBudgets().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun saveBudget(budget: Budget) {
        val entity = budget.toEntity().copy(
            id = if (budget.id.isEmpty()) UUID.randomUUID().toString() else budget.id
        )
        expenseDao.insertBudget(entity)
    }

    suspend fun deleteBudget(budgetId: String) {
        expenseDao.deleteBudget(BudgetEntity(id = budgetId, categoryId = "", amountLimit = 0.0, month = 0, year = 0))
    }

    // ==========================================
    // --- RECURRING EXPENSES ---
    // ==========================================
    fun getRecurringExpenses(): Flow<List<RecurringExpense>> {
        return expenseDao.getAllRecurringExpenses().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun saveRecurring(recurring: RecurringExpense) {
        val entity = recurring.toEntity().copy(
            id = if (recurring.id.isEmpty()) UUID.randomUUID().toString() else recurring.id
        )
        expenseDao.insertRecurringExpense(entity)
    }

    suspend fun deleteRecurring(recurringId: String) {
        expenseDao.deleteRecurringExpense(RecurringExpenseEntity(id = recurringId, amount = 0.0, note = "", categoryId = "", walletId = "", isIncome = false, nextExecutionTime = 0L))
    }

    // --- MAPPERS ---
    private fun TransactionEntity.toModel() = Transaction(id, amount, note, date, categoryId, isIncome, walletId, isTransfer, toWalletId, currency)
    private fun Transaction.toEntity() = TransactionEntity(id, amount, note, date, categoryId, isIncome, walletId, isTransfer, toWalletId, currency)

    private fun CategoryEntity.toModel() = Category(id, name, iconName, colorHex, isIncome)
    private fun Category.toEntity() = CategoryEntity(id, name, iconName, colorHex, isIncome)

    private fun WalletEntity.toModel() = Wallet(id, name, colorHex)
    private fun Wallet.toEntity() = WalletEntity(id, name, colorHex)

    private fun BudgetEntity.toModel() = Budget(id, categoryId, amountLimit, month, year)
    private fun Budget.toEntity() = BudgetEntity(id, categoryId, amountLimit, month, year)

    private fun RecurringExpenseEntity.toModel() = RecurringExpense(id, amount, note, categoryId, walletId, isIncome, nextExecutionTime, isActive)
    private fun RecurringExpense.toEntity() = RecurringExpenseEntity(id, amount, note, categoryId, walletId, isIncome, nextExecutionTime, isActive)
}
