package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetDatabase
import com.example.data.model.BudgetLimitEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.BudgetRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategorySpendProgress(
    val category: TransactionCategory,
    val spentAmount: Double,
    val limitAmount: Double,
    val percentage: Float
)

data class SpenderDistribution(
    val spender: String,
    val amount: Double,
    val percentage: Float
)

data class DailySpendPoint(
    val day: Int,
    val amount: Double
)

data class BudgetUiState(
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-based (Calendar.MONTH)
    val partner1Name: String = "Ben",
    val partner2Name: String = "Eşim",
    val transactions: List<TransactionEntity> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val totalBudgetLimit: Double = 0.0,
    val savingsRate: Float = 0f,
    val categoryProgressList: List<CategorySpendProgress> = emptyList(),
    val spenderDistribution: List<SpenderDistribution> = emptyList(),
    val dailyTrend: List<DailySpendPoint> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val recurringBills: List<RecurringBillEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: TransactionType? = null,
    val selectedSpenderFilter: String? = null,
    val selectedCategoryFilter: TransactionCategory? = null,
    val isLoading: Boolean = false
) {
    val monthTitle: String
        get() {
            val cal = Calendar.getInstance()
            cal.set(selectedYear, selectedMonth, 1)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale("tr", "TR"))
            return formatter.format(cal.time).replaceFirstChar { it.uppercase() }
        }
}

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository

    // State holders
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    private val _partner1Name = MutableStateFlow("Ben")
    private val _partner2Name = MutableStateFlow("Eşim")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _selectedSpenderFilter = MutableStateFlow<String?>(null)
    private val _selectedCategoryFilter = MutableStateFlow<TransactionCategory?>(null)

    init {
        val db = BudgetDatabase.getDatabase(application)
        repository = BudgetRepository(db.budgetDao())

        // Prepopulate demo data on first launch to deliver immediate rich graphics
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(_partner1Name.value, _partner2Name.value)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodTransactions = _selectedCalendar.flatMapLatest { cal ->
        val startCal = cal.clone() as Calendar
        startCal.set(Calendar.DAY_OF_MONTH, 1)
        startCal.set(Calendar.HOUR_OF_DAY, 0)
        startCal.set(Calendar.MINUTE, 0)
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = cal.clone() as Calendar
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        repository.getTransactionsForPeriod(startCal.timeInMillis, endCal.timeInMillis)
    }

    private val budgetLimits = repository.getAllBudgetLimits()
    private val savingsGoals = repository.getAllSavingsGoals()
    private val recurringBills = repository.getAllRecurringBills()

    val uiState: StateFlow<BudgetUiState> = combine(
        _selectedCalendar,
        periodTransactions,
        budgetLimits,
        savingsGoals,
        recurringBills,
        _partner1Name,
        _partner2Name,
        _searchQuery,
        _selectedTypeFilter,
        _selectedSpenderFilter,
        _selectedCategoryFilter
    ) { args ->
        val cal = args[0] as Calendar
        val transactions = args[1] as List<TransactionEntity>
        val limits = args[2] as List<BudgetLimitEntity>
        val goals = args[3] as List<SavingsGoalEntity>
        val bills = args[4] as List<RecurringBillEntity>
        val p1Name = args[5] as String
        val p2Name = args[6] as String
        val query = args[7] as String
        val typeFilter = args[8] as TransactionType?
        val spenderFilter = args[9] as String?
        val categoryFilter = args[10] as TransactionCategory?

        // Filter transactions for search and chips
        val filteredTransactions = transactions.filter { tx ->
            val matchQuery = query.isBlank() || tx.title.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.category.titleTr.contains(query, ignoreCase = true)
            val matchType = typeFilter == null || tx.type == typeFilter
            val matchSpender = spenderFilter == null || tx.spender == spenderFilter
            val matchCat = categoryFilter == null || tx.category == categoryFilter

            matchQuery && matchType && matchSpender && matchCat
        }

        var incomeTotal = 0.0
        var expenseTotal = 0.0
        val categorySpentMap = mutableMapOf<TransactionCategory, Double>()
        val spenderSpentMap = mutableMapOf<String, Double>()

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyMap = mutableMapOf<Int, Double>()
        for (i in 1..daysInMonth) {
            dailyMap[i] = 0.0
        }

        transactions.forEach { tx ->
            if (tx.type == TransactionType.INCOME) {
                incomeTotal += tx.amount
            } else {
                expenseTotal += tx.amount
                categorySpentMap[tx.category] = (categorySpentMap[tx.category] ?: 0.0) + tx.amount
                spenderSpentMap[tx.spender] = (spenderSpentMap[tx.spender] ?: 0.0) + tx.amount

                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                val dayOfMonth = txCal.get(Calendar.DAY_OF_MONTH)
                dailyMap[dayOfMonth] = (dailyMap[dayOfMonth] ?: 0.0) + tx.amount
            }
        }

        val balance = incomeTotal - expenseTotal
        val savingsRate = if (incomeTotal > 0) {
            ((balance / incomeTotal) * 100).toFloat().coerceIn(-100f, 100f)
        } else 0f

        val limitsMap = limits.associate { it.categoryName to it.monthlyLimit }
        val totalConfiguredLimit = limits.sumOf { it.monthlyLimit }

        // Category progress
        val categoryProgress = TransactionCategory.entries
            .filter { it != TransactionCategory.SALARY && it != TransactionCategory.ADDITIONAL_INCOME }
            .map { cat ->
                val spent = categorySpentMap[cat] ?: 0.0
                val limit = limitsMap[cat.name] ?: 0.0
                val pct = if (limit > 0) (spent / limit).toFloat() else 0f
                CategorySpendProgress(
                    category = cat,
                    spentAmount = spent,
                    limitAmount = limit,
                    percentage = pct
                )
            }
            .filter { it.spentAmount > 0 || it.limitAmount > 0 }
            .sortedByDescending { it.spentAmount }

        // Spender distribution
        val spenderDistribution = listOf(p1Name, p2Name, "Ortak").map { spender ->
            val spent = spenderSpentMap[spender] ?: 0.0
            val pct = if (expenseTotal > 0) (spent / expenseTotal).toFloat() else 0f
            SpenderDistribution(
                spender = spender,
                amount = spent,
                percentage = pct
            )
        }

        // Daily trend
        val dailyPoints = (1..daysInMonth).map { day ->
            DailySpendPoint(day = day, amount = dailyMap[day] ?: 0.0)
        }

        BudgetUiState(
            selectedYear = cal.get(Calendar.YEAR),
            selectedMonth = cal.get(Calendar.MONTH),
            partner1Name = p1Name,
            partner2Name = p2Name,
            transactions = filteredTransactions,
            totalIncome = incomeTotal,
            totalExpense = expenseTotal,
            currentBalance = balance,
            totalBudgetLimit = totalConfiguredLimit,
            savingsRate = savingsRate,
            categoryProgressList = categoryProgress,
            spenderDistribution = spenderDistribution,
            dailyTrend = dailyPoints,
            savingsGoals = goals,
            recurringBills = bills,
            searchQuery = query,
            selectedTypeFilter = typeFilter,
            selectedSpenderFilter = spenderFilter,
            selectedCategoryFilter = categoryFilter,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(isLoading = true)
    )

    // Month navigation
    fun selectPreviousMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, -1)
        _selectedCalendar.value = cal
    }

    fun selectNextMonth() {
        val cal = _selectedCalendar.value.clone() as Calendar
        cal.add(Calendar.MONTH, 1)
        _selectedCalendar.value = cal
    }

    fun selectCurrentMonth() {
        _selectedCalendar.value = Calendar.getInstance()
    }

    // Filters
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: TransactionType?) {
        _selectedTypeFilter.value = if (_selectedTypeFilter.value == type) null else type
    }

    fun setSpenderFilter(spender: String?) {
        _selectedSpenderFilter.value = if (_selectedSpenderFilter.value == spender) null else spender
    }

    fun setCategoryFilter(category: TransactionCategory?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setPartnerNames(name1: String, name2: String) {
        _partner1Name.value = name1.ifBlank { "Ben" }
        _partner2Name.value = name2.ifBlank { "Eşim" }
    }

    // CRUD Transactions
    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        spender: String,
        timestamp: Long = System.currentTimeMillis(),
        paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
        note: String = ""
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                spender = spender,
                timestamp = timestamp,
                paymentMethod = paymentMethod,
                note = note.trim()
            )
            repository.insertTransaction(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Budgets & Goals
    fun setCategoryLimit(category: TransactionCategory, limit: Double) {
        viewModelScope.launch {
            repository.setBudgetLimit(category, limit)
        }
    }

    fun addSavingsGoal(title: String, targetAmount: Double, initialDeposit: Double = 0.0) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                title = title.trim(),
                targetAmount = targetAmount,
                currentAmount = initialDeposit,
                icon = "savings"
            )
            repository.insertSavingsGoal(goal)
        }
    }

    fun adjustGoalAmount(goal: SavingsGoalEntity, delta: Double) {
        viewModelScope.launch {
            val updated = goal.copy(
                currentAmount = (goal.currentAmount + delta).coerceAtLeast(0.0)
            )
            repository.updateSavingsGoal(updated)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // Bills
    fun addRecurringBill(title: String, amount: Double, dueDay: Int, spender: String = "Ortak") {
        viewModelScope.launch {
            val bill = RecurringBillEntity(
                title = title.trim(),
                amount = amount,
                dueDay = dueDay.coerceIn(1, 31),
                isPaid = false,
                spender = spender
            )
            repository.insertRecurringBill(bill)
        }
    }

    fun toggleBillPaid(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.toggleBillPaid(bill.id, !bill.isPaid)
        }
    }

    fun deleteRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.deleteRecurringBill(bill)
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.clearAllTransactions()
            repository.seedInitialDataIfEmpty(_partner1Name.value, _partner2Name.value)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }
}

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    return formatter.format(amount)
}

fun formatCompactCurrency(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format(Locale("tr", "TR"), "%.1fM ₺", amount / 1_000_000)
        amount >= 1_000 -> String.format(Locale("tr", "TR"), "%.1fB ₺", amount / 1_000)
        else -> String.format(Locale("tr", "TR"), "%.0f ₺", amount)
    }
}
