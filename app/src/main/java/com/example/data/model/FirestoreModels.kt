package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class FamilyBudgetDoc(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val familyCode: String = "",
    val passcode: String = "",
    val memberUids: List<String> = emptyList(),
    val spouse1Name: String = "Ben",
    val spouse2Name: String = "Eşim",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

@IgnoreExtraProperties
data class FirestoreTransactionDoc(
    @DocumentId
    val id: String = "",
    val familyId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "EXPENSE",
    val category: String = "MARKET",
    val spender: String = "Ortak",
    val createdByUid: String = "",
    val createdByName: String = "",
    val timestamp: Long = 0L,
    val paymentMethod: String = "CREDIT_CARD",
    val note: String = "",
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

@IgnoreExtraProperties
data class FirestoreLimitDoc(
    @DocumentId
    val categoryName: String = "",
    val monthlyLimit: Double = 0.0,
    @ServerTimestamp
    val updatedAt: Date? = null
)

@IgnoreExtraProperties
data class FirestoreSavingsGoalDoc(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val icon: String = "savings",
    @ServerTimestamp
    val updatedAt: Date? = null
)

@IgnoreExtraProperties
data class FirestoreBillDoc(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val dueDay: Long = 1L,
    val isPaid: Boolean = false,
    val spender: String = "Ortak",
    @ServerTimestamp
    val updatedAt: Date? = null
)
