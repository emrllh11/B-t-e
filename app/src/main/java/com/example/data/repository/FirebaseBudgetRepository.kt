package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FamilyBudgetDoc
import com.example.data.model.FirestoreBillDoc
import com.example.data.model.FirestoreLimitDoc
import com.example.data.model.FirestoreSavingsGoalDoc
import com.example.data.model.FirestoreTransactionDoc
import com.example.data.model.PaymentMethod
import com.example.data.model.TransactionCategory
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseBudgetRepository(
    private val db: FirebaseFirestore,
    private val localRoomRepository: BudgetRepository
) {
    constructor(context: Context, localRoomRepo: BudgetRepository) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        localRoomRepo
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("İşlem yapabilmek için Google ile oturum açılmış olmalıdır.")
    }

    /**
     * Create a new Family Budget document in Firestore and initialize defaults
     */
    suspend fun createFamilyBudget(
        name: String,
        familyCode: String,
        passcode: String,
        mySpouseName: String,
        partnerSpouseName: String
    ): FamilyBudgetDoc {
        val uid = requireUserId()
        val familyId = UUID.randomUUID().toString()
        val cleanCode = familyCode.trim().uppercase()

        val familyDoc = FamilyBudgetDoc(
            id = familyId,
            name = name.trim().ifBlank { "Sağlam Aile Bütçesi" },
            familyCode = cleanCode,
            passcode = passcode.trim(),
            memberUids = listOf(uid),
            spouse1Name = mySpouseName.trim().ifBlank { "Ben" },
            spouse2Name = partnerSpouseName.trim().ifBlank { "Eşim" }
        )

        val docRef = db.collection("familyBudgets").document(familyId)
        try {
            docRef.set(familyDoc).await()
            return familyDoc
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    /**
     * Find and join an existing Family Budget using the shared Family Code and Password/PIN
     */
    suspend fun joinFamilyBudget(
        familyCode: String,
        passcode: String,
        myName: String
    ): FamilyBudgetDoc {
        val uid = requireUserId()
        val cleanCode = familyCode.trim().uppercase()
        val cleanPass = passcode.trim()

        val query = db.collection("familyBudgets")
            .whereEqualTo("familyCode", cleanCode)
            .limit(1)

        try {
            val snapshot = query.get().await()
            if (snapshot.isEmpty) {
                throw IllegalArgumentException("Bu aile kodu ($cleanCode) ile eşleşen bir bütçe bulunamadı.")
            }

            val doc = snapshot.documents.first()
            val family = doc.toObject(FamilyBudgetDoc::class.java)
                ?: throw IllegalStateException("Aile bütçesi verisi okunamadı.")

            if (family.passcode != cleanPass) {
                throw IllegalArgumentException("Aile bütçesi güvenlik şifresi/PIN hatalı!")
            }

            // If user is not yet in memberUids, add them
            if (!family.memberUids.contains(uid)) {
                val updatedMembers = family.memberUids + uid
                doc.reference.update(
                    mapOf(
                        "memberUids" to updatedMembers,
                        "spouse2Name" to myName.ifBlank { family.spouse2Name },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                return family.copy(
                    memberUids = updatedMembers,
                    spouse2Name = myName.ifBlank { family.spouse2Name }
                )
            }

            return family
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "familyBudgets")
            throw e
        }
    }

    /**
     * Real-time observable Flow of Transactions for this Family Budget
     */
    fun observeFamilyTransactions(familyId: String): Flow<List<TransactionEntity>> {
        val collectionRef = db.collection("familyBudgets").document(familyId).collection("transactions")
        return collectionRef
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FirestoreTransactionDoc::class.java).map { doc ->
                    TransactionEntity(
                        id = doc.id.hashCode().toLong(),
                        title = doc.title,
                        amount = doc.amount,
                        type = try { TransactionType.valueOf(doc.type) } catch (_: Exception) { TransactionType.EXPENSE },
                        category = try { TransactionCategory.valueOf(doc.category) } catch (_: Exception) { TransactionCategory.MARKET },
                        spender = doc.spender,
                        timestamp = if (doc.timestamp > 0) doc.timestamp else System.currentTimeMillis(),
                        paymentMethod = try { PaymentMethod.valueOf(doc.paymentMethod) } catch (_: Exception) { PaymentMethod.CREDIT_CARD },
                        note = doc.note
                    )
                }
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.LIST, collectionRef.path)
                }
                throw error
            }
    }

    /**
     * Real-time observable Flow of the Family Budget document (for spouse names, member count)
     */
    fun observeFamilyBudget(familyId: String): Flow<FamilyBudgetDoc?> {
        val docRef = db.collection("familyBudgets").document(familyId)
        return docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(FamilyBudgetDoc::class.java)
            }
            .catch { error ->
                if (error is Exception) {
                    handleFirestoreError(error, OperationType.GET, docRef.path)
                }
                throw error
            }
    }

    /**
     * Add a transaction to cloud and sync to Room
     */
    suspend fun addTransaction(
        familyId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        category: TransactionCategory,
        spender: String,
        timestamp: Long,
        paymentMethod: PaymentMethod,
        note: String
    ) {
        val uid = requireUserId()
        val txId = UUID.randomUUID().toString()

        val doc = FirestoreTransactionDoc(
            id = txId,
            familyId = familyId,
            title = title.trim(),
            amount = amount,
            type = type.name,
            category = category.name,
            spender = spender,
            createdByUid = uid,
            createdByName = auth.currentUser?.displayName ?: spender,
            timestamp = timestamp,
            paymentMethod = paymentMethod.name,
            note = note.trim()
        )

        val docRef = db.collection("familyBudgets").document(familyId).collection("transactions").document(txId)
        try {
            docRef.set(doc).await()
            // Also store in Room
            localRoomRepository.insertTransaction(
                TransactionEntity(
                    id = txId.hashCode().toLong(),
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTransaction(familyId: String, transactionId: String) {
        val docRef = db.collection("familyBudgets").document(familyId).collection("transactions").document(transactionId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }
}
