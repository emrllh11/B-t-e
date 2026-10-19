package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BudgetDatabase
import com.example.data.model.BudgetLimitEntity
import com.example.data.model.FamilyBudgetDoc
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.BudgetRepository
import com.example.data.repository.FirebaseBudgetRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH), // 0-based
    val partner1Name: String = "Ben",
    val partner2Name: String = "Eşim",
    val activeFamily: FamilyBudgetDoc? = null,
    val isFamilyConnected: Boolean = false,
    val familyError: String? = null,
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

    private val localRepository: BudgetRepository
    private val firebaseRepository: FirebaseBudgetRepository
    private val prefs = application.getSharedPreferences("family_budget_prefs", Context.MODE_PRIVATE)

    // State holders
    private val _selectedCalendar = MutableStateFlow(Calendar.getInstance())
    private val _activeFamilyId = MutableStateFlow<String?>(prefs.getString("active_family_id", null))
    private val _familyError = MutableStateFlow<String?>(null)
    private val _partner1Name = MutableStateFlow("Ben")
    private val _partner2Name = MutableStateFlow("Eşim")
    private val _searchQuery = MutableStateFlow("")
    private val _selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    private val _selectedSpenderFilter = MutableStateFlow<String?>(null)
    private val _selectedCategoryFilter = MutableStateFlow<TransactionCategory?>(null)

    init {
        val db = BudgetDatabase.getDatabase(application)
        localRepository = BudgetRepository(db.budgetDao())
        firebaseRepository = FirebaseBudgetRepository(application, localRepository)

        // Prepopulate demo data on first launch
        viewModelScope.launch {
            localRepository.seedInitialDataIfEmpty(_partner1Name.value, _partner2Name.value)
        }
    }

    // Active Family Document Flow (from Firestore if connected)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeFamilyFlow: Flow<FamilyBudgetDoc?> = _activeFamilyId.flatMapLatest { familyId ->
        if (familyId != null && Firebase.auth.currentUser != null) {
            firebaseRepository.observeFamilyBudget(familyId)
        } else {
            flowOf(null)
        }
    }

    // Real-time Transactions: Streams from Firestore if a family budget is linked, or fallback to Room
    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodTransactions: Flow<List<TransactionEntity>> = combine(
        _selectedCalendar,
        _activeFamilyId
    ) { cal, familyId ->
        cal to familyId
    }.flatMapLatest { (cal, familyId) ->
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

        if (familyId != null && Firebase.auth.currentUser != null) {
            firebaseRepository.observeFamilyTransactions(familyId).map { list ->
                list.filter { it.timestamp in startCal.timeInMillis..endCal.timeInMillis }
            }
        } else {
            localRepository.getTransactionsForPeriod(startCal.timeInMillis, endCal.timeInMillis)
        }
    }

    private val budgetLimits = localRepository.getAllBudgetLimits()
    private val savingsGoals = localRepository.getAllSavingsGoals()
    private val recurringBills = localRepository.getAllRecurringBills()

    val uiState: StateFlow<BudgetUiState> = combine(
        _selectedCalendar,
        periodTransactions,
        budgetLimits,
        savingsGoals,
        recurringBills,
        activeFamilyFlow,
        _partner1Name,
        _partner2Name,
        _searchQuery,
        _selectedTypeFilter,
        _selectedSpenderFilter,
        _selectedCategoryFilter,
        _familyError
    ) { args ->
        val cal = args[0] as Calendar
        val transactions = args[1] as List<TransactionEntity>
        val limits = args[2] as List<BudgetLimitEntity>
        val goals = args[3] as List<SavingsGoalEntity>
        val bills = args[4] as List<RecurringBillEntity>
        val activeFamily = args[5] as FamilyBudgetDoc?
        val p1Name = activeFamily?.spouse1Name ?: (args[6] as String)
        val p2Name = activeFamily?.spouse2Name ?: (args[7] as String)
        val query = args[8] as String
        val typeFilter = args[9] as TransactionType?
        val spenderFilter = args[10] as String?
        val categoryFilter = args[11] as TransactionCategory?
        val familyErr = args[12] as String?

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
            activeFamily = activeFamily,
            isFamilyConnected = activeFamily != null,
            familyError = familyErr,
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

    // Family Creation & Joint Sharing
    fun createFamily(
        name: String,
        code: String,
        passcode: String,
        myName: String,
        partnerName: String
    ) {
        viewModelScope.launch {
            _familyError.value = null
            try {
                val family = firebaseRepository.createFamilyBudget(
                    name = name,
                    familyCode = code,
                    passcode = passcode,
                    mySpouseName = myName,
                    partnerSpouseName = partnerName
                )
                prefs.edit().putString("active_family_id", family.id).apply()
                _activeFamilyId.value = family.id
            } catch (e: Exception) {
                _familyError.value = e.localizedMessage ?: "Aile bütçesi oluşturulamadı."
            }
        }
    }

    fun joinFamily(code: String, passcode: String, myName: String) {
        viewModelScope.launch {
            _familyError.value = null
            try {
                val family = firebaseRepository.joinFamilyBudget(
                    familyCode = code,
                    passcode = passcode,
                    myName = myName
                )
                prefs.edit().putString("active_family_id", family.id).apply()
                _activeFamilyId.value = family.id
            } catch (e: Exception) {
                _familyError.value = e.localizedMessage ?: "Aile bütçesine bağlanılamadı."
            }
        }
    }

    fun leaveFamily() {
        prefs.edit().remove("active_family_id").apply()
        _activeFamilyId.value = null
    }

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

    // CRUD Transactions with Real-time Cloud Sync
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
            val familyId = _activeFamilyId.value
            if (familyId != null && Firebase.auth.currentUser != null) {
                try {
                    firebaseRepository.addTransaction(
                        familyId = familyId,
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        spender = spender,
                        timestamp = timestamp,
                        paymentMethod = paymentMethod,
                        note = note
                    )
                } catch (e: Exception) {
                    // Fallback to local if network error
                    localRepository.insertTransaction(
                        TransactionEntity(
                            title = title,
                            amount = amount,
                            type = type,
                            category = category,
                            spender = spender,
                            timestamp = timestamp,
                            paymentMethod = paymentMethod,
                            note = note
                        )
                    )
                }
            } else {
                localRepository.insertTransaction(
                    TransactionEntity(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        spender = spender,
                        timestamp = timestamp,
                        paymentMethod = paymentMethod,
                        note = note
                    )
                )
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            localRepository.deleteTransaction(transaction)
        }
    }

    // Budgets & Goals
    fun setCategoryLimit(category: TransactionCategory, limit: Double) {
        viewModelScope.launch {
            localRepository.setBudgetLimit(category, limit)
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
            localRepository.insertSavingsGoal(goal)
        }
    }

    fun adjustGoalAmount(goal: SavingsGoalEntity, delta: Double) {
        viewModelScope.launch {
            val updated = goal.copy(
                currentAmount = (goal.currentAmount + delta).coerceAtLeast(0.0)
            )
            localRepository.updateSavingsGoal(updated)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            localRepository.deleteSavingsGoal(goal)
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
            localRepository.insertRecurringBill(bill)
        }
    }

    fun toggleBillPaid(bill: RecurringBillEntity) {
        viewModelScope.launch {
            localRepository.toggleBillPaid(bill.id, !bill.isPaid)
        }
    }

    fun deleteRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            localRepository.deleteRecurringBill(bill)
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            localRepository.clearAllTransactions()
            localRepository.seedInitialDataIfEmpty(_partner1Name.value, _partner2Name.value)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            localRepository.clearAllTransactions()
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
