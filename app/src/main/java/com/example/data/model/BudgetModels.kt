package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionType {
    EXPENSE,
    INCOME
}

enum class TransactionCategory(
    val titleTr: String,
    val iconName: String,
    val defaultColorHex: Long
) {
    MARKET("Market & Mutfak", "shopping_cart", 0xFF10B981),
    BILLS("Fatura & Aidat", "receipt", 0xFF3B82F6),
    HOUSING("Ev & Kira", "home", 0xFF8B5CF6),
    TRANSPORT("Ulaşım & Yakıt", "directions_car", 0xFFF59E0B),
    EATING_OUT("Yeme & İçme", "restaurant", 0xFFEF4444),
    ENTERTAINMENT("Eğlence & Tatil", "celebration", 0xFFEC4899),
    HEALTH("Sağlık & Bakım", "medical_services", 0xFF06B6D4),
    CHILDREN("Çocuk & Eğitim", "child_care", 0xFF84CC16),
    CLOTHING("Giyim & Moda", "checkroom", 0xFFF97316),
    SALARY("Maaş", "payments", 0xFF10B981),
    ADDITIONAL_INCOME("Ek Gelir / Prim", "trending_up", 0xFF059669),
    OTHER("Diğer", "category", 0xFF6B7280)
}

enum class PaymentMethod(val titleTr: String) {
    CREDIT_CARD("Kredi Kartı"),
    DEBIT_CARD("Banka Kartı"),
    CASH("Nakit"),
    BANK_TRANSFER("Havale / EFT")
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: TransactionCategory,
    val spender: String, // "Ben", "Eşim", "Ortak"
    val timestamp: Long,
    val paymentMethod: PaymentMethod = PaymentMethod.CREDIT_CARD,
    val note: String = ""
)

@Entity(tableName = "budget_limits")
data class BudgetLimitEntity(
    @PrimaryKey
    val categoryName: String, // matches TransactionCategory.name
    val monthlyLimit: Double
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val icon: String = "savings"
)

@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDay: Int, // 1 - 31
    val isPaid: Boolean = false,
    val spender: String = "Ortak"
)
