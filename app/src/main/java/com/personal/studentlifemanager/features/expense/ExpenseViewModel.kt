package com.personal.studentlifemanager.features.expense

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.personal.studentlifemanager.core.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpenseRepository(AppDatabase.getDatabase(application).expenseDao())

    var allTransactions by mutableStateOf<List<Transaction>>(emptyList())
        private set
    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var wallets by mutableStateOf<List<Wallet>>(emptyList())
        private set
    var budgets by mutableStateOf<List<Budget>>(emptyList())
        private set
    var recurringExpenses by mutableStateOf<List<RecurringExpense>>(emptyList())
        private set

    var searchQuery by mutableStateOf("")

    var selectedMonth by mutableStateOf(Calendar.getInstance().get(Calendar.MONTH))
    var selectedYear by mutableStateOf(Calendar.getInstance().get(Calendar.YEAR))
    var isBalanceHidden by mutableStateOf(false)
        private set

    var exchangeRates by mutableStateOf<Map<String, Double>>(emptyMap())
        private set
    var supportedCurrencies by mutableStateOf<List<String>>(listOf("VND"))
        private set

    init {
        viewModelScope.launch {
            repository.getTransactions().collectLatest { allTransactions = it }
        }
        viewModelScope.launch {
            repository.getCategories().collectLatest { categories = it }
        }
        viewModelScope.launch {
            repository.getWallets().collectLatest { wallets = it }
        }
        viewModelScope.launch {
            repository.getBudgets().collectLatest { budgets = it }
        }
        viewModelScope.launch {
            repository.getRecurringExpenses().collectLatest { recurringExpenses = it }
        }
        fetchExchangeRates()
    }

    val filteredTransactions: List<Transaction>
        get() = allTransactions.filter {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it.date
            cal.get(Calendar.MONTH) == selectedMonth && cal.get(Calendar.YEAR) == selectedYear &&
            (searchQuery.isEmpty() || it.note.contains(searchQuery, ignoreCase = true))
        }

    fun previousMonth() {
        if (selectedMonth == 0) {
            selectedMonth = 11
            selectedYear -= 1
        } else {
            selectedMonth -= 1
        }
    }

    fun nextMonth() {
        if (selectedMonth == 11) {
            selectedMonth = 0
            selectedYear += 1
        } else {
            selectedMonth += 1
        }
    }

    fun toggleBalanceVisibility() {
        isBalanceHidden = !isBalanceHidden
    }

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.addTransaction(transaction) }
    }

    fun addTransaction(amount: Double, note: String, categoryId: String, walletId: String, isIncome: Boolean, currency: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.addTransaction(Transaction(amount = amount, note = note, categoryId = categoryId, walletId = walletId, isIncome = isIncome, currency = currency))
            onSaved()
        }
    }

    fun addTransfer(amount: Double, note: String, fromWalletId: String, toWalletId: String, date: Long, currency: String, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.addTransaction(Transaction(amount = amount, note = note, walletId = fromWalletId, toWalletId = toWalletId, isTransfer = true, date = date, currency = currency))
            onSaved()
        }
    }

    fun updateTransaction(transaction: Transaction, onSaved: () -> Unit = {}) {
        viewModelScope.launch { 
            repository.updateTransaction(transaction)
            onSaved()
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch { repository.deleteTransaction(transactionId) }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch { repository.addCategory(category) }
    }
    
    fun addCategory(name: String, colorHex: String, isIncome: Boolean, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.addCategory(Category(name = name, iconName = "Custom", colorHex = colorHex, isIncome = isIncome))
            onSaved()
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch { repository.deleteCategory(categoryId) }
    }

    fun addWallet(wallet: Wallet) {
        viewModelScope.launch { repository.addWallet(wallet) }
    }

    fun deleteWallet(walletId: String) {
        viewModelScope.launch { repository.deleteWallet(walletId) }
    }

    fun saveBudget(budget: Budget) {
        viewModelScope.launch { repository.saveBudget(budget) }
    }

    fun saveBudget(categoryId: String, limit: Double, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveBudget(Budget(categoryId = categoryId, amountLimit = limit, month = selectedMonth, year = selectedYear))
            onSaved()
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch { repository.deleteBudget(budgetId) }
    }

    fun saveRecurring(recurring: RecurringExpense) {
        viewModelScope.launch { repository.saveRecurring(recurring) }
    }

    fun saveRecurring(recurring: RecurringExpense, onDismiss: () -> Unit) {
        viewModelScope.launch {
            repository.saveRecurring(recurring)
            onDismiss()
        }
    }

    fun deleteRecurring(recurringId: String) {
        viewModelScope.launch { repository.deleteRecurring(recurringId) }
    }

    fun toggleRecurringState(recurring: RecurringExpense) {
        viewModelScope.launch {
            repository.saveRecurring(recurring.copy(isActive = !recurring.isActive))
        }
    }

    // ==========================================
    // --- KHU VỰC 6: AI OCR QUÉT HÓA ĐƠN ---
    // ==========================================
    fun processReceiptImage(context: Context, uri: Uri, onResult: (String) -> Unit, onError: () -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    val extractedAmount = extractMaxAmount(text)
                    if (extractedAmount > 0) {
                        onResult(extractedAmount.toLong().toString())
                    } else {
                        onError()
                    }
                }
                .addOnFailureListener {
                    onError()
                }
        } catch (e: Exception) {
            onError()
        }
    }

    private fun extractMaxAmount(text: String): Double {
        val regex = Regex("""\b[1-9]\d{0,2}(?:[.,]\d{3})+\b|\b[1-9]\d{3,8}\b""")
        val matches = regex.findAll(text)

        var maxAmount = 0.0
        for (match in matches) {
            val numStr = match.value.replace(",", "").replace(".", "")
            val num = numStr.toDoubleOrNull() ?: 0.0

            if (num > 1000 && num < 1000000000 && num % 100 == 0.0) {
                if (num > maxAmount) {
                    maxAmount = num
                }
            }
        }
        return maxAmount
    }

    // ==========================================
    // --- KHU VỰC 7: AI PHÂN TÍCH & DỰ ĐOÁN ---
    // ==========================================
    fun isAbnormalExpense(amount: Double): Boolean {
        val expenseList = allTransactions.filter { !it.isIncome && !it.isTransfer }
        if (expenseList.isEmpty()) return false

        val avgExpense = expenseList.sumOf { it.amount } / expenseList.size
        return amount > (avgExpense * 3) && amount > 500000.0
    }

    fun getPredictedEndOfMonthExpense(): Double {
        val currentMonthExpenses = filteredTransactions.filter { !it.isIncome && !it.isTransfer }.sumOf { it.amount }

        val cal = Calendar.getInstance()
        val todayMonth = cal.get(Calendar.MONTH)
        val todayYear = cal.get(Calendar.YEAR)

        if (selectedMonth == todayMonth && selectedYear == todayYear) {
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            if (currentDay == 0) return currentMonthExpenses
            val dailyBurnRate = currentMonthExpenses / currentDay
            return dailyBurnRate * maxDays
        }
        return currentMonthExpenses
    }

    fun getExpenseForMonth(month: Int, year: Int): Double {
        return allTransactions.filter { 
            !it.isIncome && !it.isTransfer && 
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.MONTH) == month &&
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.YEAR) == year
        }.sumOf { it.amount }
    }

    // ==========================================
    // --- KHU VỰC 8: XỬ LÝ ĐA TIỀN TỆ (MULTI-CURRENCY) ---
    // ==========================================
    private fun fetchExchangeRates() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://open.er-api.com/v6/latest/VND")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connect()
                val response = connection.inputStream.bufferedReader().readText()
                val json = org.json.JSONObject(response)
                val rates = json.getJSONObject("rates")

                val map = mutableMapOf<String, Double>()
                val currencyList = mutableListOf<String>()

                rates.keys().forEach { curr ->
                    currencyList.add(curr)
                    map[curr] = rates.getDouble(curr)
                }

                val sortedList = currencyList.sorted().toMutableList()
                sortedList.remove("VND")
                sortedList.add(0, "VND")

                withContext(Dispatchers.Main) {
                    exchangeRates = map
                    supportedCurrencies = sortedList
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun getWalletBalancesMulti(walletId: String): Map<String, Double> {
        val balances = mutableMapOf<String, Double>()
        allTransactions.filter { it.walletId == walletId }.forEach { t ->
            val current = balances[t.currency] ?: 0.0
            if (t.isTransfer) {
                if (t.walletId == walletId) balances[t.currency] = current - t.amount
            } else {
                balances[t.currency] = if (t.isIncome) current + t.amount else current - t.amount
            }
        }
        allTransactions.filter { it.toWalletId == walletId && it.isTransfer }.forEach { t ->
            balances[t.currency] = (balances[t.currency] ?: 0.0) + t.amount
        }
        return balances.filterValues { it != 0.0 }
    }

    fun convertToVND(amount: Double, currency: String): Double {
        if (currency == "VND") return amount
        val rate = exchangeRates[currency] ?: return 0.0
        return amount / rate
    }
}
