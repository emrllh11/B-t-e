package com.example.data.repository

import com.example.data.local.BudgetDao
import com.example.data.model.BudgetLimitEntity
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringBillEntity
import com.example.data.model.SavingsGoalEntity
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class BudgetRepository(private val dao: BudgetDao) {

    fun getAllTransactions(): Flow<List<TransactionEntity>> = dao.getAllTransactions()

    fun getTransactionsForPeriod(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> =
        dao.getTransactionsForPeriod(startTime, endTime)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        dao.insertTransaction(transaction)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        dao.updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        dao.deleteTransaction(transaction)

    suspend fun clearAllTransactions() = dao.deleteAllTransactions()

    fun getAllBudgetLimits(): Flow<List<BudgetLimitEntity>> = dao.getAllBudgetLimits()

    suspend fun setBudgetLimit(category: TransactionCategory, limit: Double) =
        dao.setBudgetLimit(BudgetLimitEntity(category.name, limit))

    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>> = dao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long = dao.insertSavingsGoal(goal)

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = dao.updateSavingsGoal(goal)

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = dao.deleteSavingsGoal(goal)

    fun getAllRecurringBills(): Flow<List<RecurringBillEntity>> = dao.getAllRecurringBills()

    suspend fun insertRecurringBill(bill: RecurringBillEntity): Long = dao.insertRecurringBill(bill)

    suspend fun updateRecurringBill(bill: RecurringBillEntity) = dao.updateRecurringBill(bill)

    suspend fun deleteRecurringBill(bill: RecurringBillEntity) = dao.deleteRecurringBill(bill)

    suspend fun toggleBillPaid(id: Long, isPaid: Boolean) = dao.setBillPaidStatus(id, isPaid)

    suspend fun seedInitialDataIfEmpty(userName1: String = "Ben", userName2: String = "Eşim") {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)

        fun createDate(day: Int, hour: Int = 14): Long {
            val cal = Calendar.getInstance()
            cal.set(year, month, day.coerceIn(1, 28), hour, 30, 0)
            return cal.timeInMillis
        }

        // Default Budget limits
        val defaultLimits = listOf(
            BudgetLimitEntity(TransactionCategory.MARKET.name, 16000.0),
            BudgetLimitEntity(TransactionCategory.HOUSING.name, 25000.0),
            BudgetLimitEntity(TransactionCategory.BILLS.name, 4500.0),
            BudgetLimitEntity(TransactionCategory.TRANSPORT.name, 6000.0),
            BudgetLimitEntity(TransactionCategory.EATING_OUT.name, 5000.0),
            BudgetLimitEntity(TransactionCategory.CLOTHING.name, 4000.0),
            BudgetLimitEntity(TransactionCategory.ENTERTAINMENT.name, 3500.0),
            BudgetLimitEntity(TransactionCategory.HEALTH.name, 3000.0)
        )
        dao.insertBudgetLimits(defaultLimits)

        // Initial Savings Goals
        val defaultGoals = listOf(
            SavingsGoalEntity(
                title = "Yaz Tatili Fonu",
                targetAmount = 45000.0,
                currentAmount = 24000.0,
                icon = "beach_access"
            ),
            SavingsGoalEntity(
                title = "Acil Durum Güvencesi",
                targetAmount = 75000.0,
                currentAmount = 52500.0,
                icon = "shield"
            ),
            SavingsGoalEntity(
                title = "Yeni Aile Arabası Peşinatı",
                targetAmount = 180000.0,
                currentAmount = 65000.0,
                icon = "directions_car"
            )
        )
        dao.insertSavingsGoals(defaultGoals)

        // Initial Recurring Bills
        val defaultBills = listOf(
            RecurringBillEntity(title = "Ev Kirası", amount = 22000.0, dueDay = 1, isPaid = true, spender = "Ortak"),
            RecurringBillEntity(title = "Elektrik Faturası", amount = 850.0, dueDay = 15, isPaid = true, spender = userName1),
            RecurringBillEntity(title = "Doğalgaz Faturası", amount = 1450.0, dueDay = 18, isPaid = false, spender = "Ortak"),
            RecurringBillEntity(title = "Ev İnterneti & TV", amount = 540.0, dueDay = 22, isPaid = false, spender = userName2),
            RecurringBillEntity(title = "Site / Apartman Aidatı", amount = 950.0, dueDay = 25, isPaid = false, spender = "Ortak")
        )
        dao.insertRecurringBills(defaultBills)

        // Initial Transactions
        val sampleTransactions = listOf(
            TransactionEntity(
                title = "Aylık Maaş (Ana Gelir)",
                amount = 58000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                spender = userName1,
                timestamp = createDate(1, 9),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                note = "Maaş yatırıldı"
            ),
            TransactionEntity(
                title = "Eşimin Maaşı",
                amount = 52000.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.SALARY,
                spender = userName2,
                timestamp = createDate(1, 10),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                note = "Ortak hesaba aktarıldı"
            ),
            TransactionEntity(
                title = "Ev Kirası Ödemesi",
                amount = 22000.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.HOUSING,
                spender = "Ortak",
                timestamp = createDate(1, 11),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                note = "Ekim ayı kirası ev sahibine ödendi"
            ),
            TransactionEntity(
                title = "Haftalık Süpermarket Alışverişi",
                amount = 3450.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.MARKET,
                spender = userName2,
                timestamp = createDate(3, 16),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                note = "Meyve, sebze, kahvaltılık"
            ),
            TransactionEntity(
                title = "Akaryakıt Dolumu",
                amount = 1750.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.TRANSPORT,
                spender = userName1,
                timestamp = createDate(5, 8),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                note = "Tam depo benzin"
            ),
            TransactionEntity(
                title = "Elektrik Faturası",
                amount = 850.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.BILLS,
                spender = userName1,
                timestamp = createDate(7, 14),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                note = "Otomatik ödeme"
            ),
            TransactionEntity(
                title = "Haftasonu Aile Yemeği",
                amount = 1850.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.EATING_OUT,
                spender = "Ortak",
                timestamp = createDate(8, 19),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                note = "Boğaz kenarı akşam yemeği"
            ),
            TransactionEntity(
                title = "Şarküteri & Kasap Alışverişi",
                amount = 2300.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.MARKET,
                spender = userName1,
                timestamp = createDate(11, 12),
                paymentMethod = PaymentMethod.DEBIT_CARD,
                note = "Et ve süt ürünleri"
            ),
            TransactionEntity(
                title = "Kışlık Mont & Bot Alışverişi",
                amount = 2800.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.CLOTHING,
                spender = userName2,
                timestamp = createDate(13, 15),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                note = "İndirim fırsatı"
            ),
            TransactionEntity(
                title = "Proje Prim Geliri",
                amount = 7500.0,
                type = TransactionType.INCOME,
                category = TransactionCategory.ADDITIONAL_INCOME,
                spender = userName1,
                timestamp = createDate(14, 11),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
                note = "Ek başarı primi"
            ),
            TransactionEntity(
                title = "Eczane & Vitamin Takviyeleri",
                amount = 920.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.HEALTH,
                spender = userName2,
                timestamp = createDate(15, 17),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                note = "Mevsim geçişi vitaminleri"
            ),
            TransactionEntity(
                title = "Sinema & Kahve Keyfi",
                amount = 640.0,
                type = TransactionType.EXPENSE,
                category = TransactionCategory.ENTERTAINMENT,
                spender = userName1,
                timestamp = createDate(16, 20),
                paymentMethod = PaymentMethod.CASH,
                note = "Eşimle birlikte"
            )
        )
        dao.insertTransactions(sampleTransactions)
    }
}
