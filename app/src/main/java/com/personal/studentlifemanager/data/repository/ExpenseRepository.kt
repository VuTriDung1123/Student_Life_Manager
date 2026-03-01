package com.personal.studentlifemanager.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.personal.studentlifemanager.data.model.Category
import com.personal.studentlifemanager.data.model.Transaction

class ExpenseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        // Bật tính năng Offline Persistence (Lưu trữ ngoại tuyến)
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
            )
            .build()
        firestore.firestoreSettings = settings
    }

    // Lấy ID của user đang đăng nhập
    private val userId get() = auth.currentUser?.uid ?: "unknown_user"

    // ==========================================
    // --- KHU VỰC 1: GIAO DỊCH (TRANSACTIONS) ---
    // ==========================================
    private val transactionRef get() = firestore
        .collection("users")
        .document(userId)
        .collection("transactions")

    fun addTransaction(transaction: Transaction, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val docRef = transactionRef.document()
        val transactionWithId = transaction.copy(id = docRef.id)

        docRef.set(transactionWithId)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun getTransactions(onResult: (List<Transaction>) -> Unit) {
        transactionRef.orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList()
                onResult(list)
            }
    }

    // 🗑️ HÀM BỊ THIẾU GÂY RA LỖI ĐÃ ĐƯỢC THÊM VÀO ĐÂY
    fun deleteTransaction(transactionId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        transactionRef.document(transactionId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // ==========================================
    // --- KHU VỰC 2: DANH MỤC (CATEGORIES) ---
    // ==========================================
    private val categoryRef get() = firestore
        .collection("users")
        .document(userId)
        .collection("categories")

    fun getCategories(onResult: (List<Category>) -> Unit) {
        categoryRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onResult(emptyList())
                return@addSnapshotListener
            }
            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.apply { id = doc.id }
            } ?: emptyList()
            onResult(list)
        }
    }

    fun addCategory(category: Category, onSuccess: () -> Unit) {
        val docRef = categoryRef.document()
        val catWithId = category.apply { id = docRef.id }
        docRef.set(catWithId).addOnSuccessListener { onSuccess() }
    }

    fun deleteCategory(categoryId: String) {
        categoryRef.document(categoryId).delete()
    }

    // 🔄 Hàm Cập nhật (Sửa) giao dịch
    fun updateTransaction(transaction: Transaction, onSuccess: () -> Unit) {
        // Dùng đúng ID của nó để ghi đè dữ liệu mới lên
        transactionRef.document(transaction.id).set(transaction)
            .addOnSuccessListener { onSuccess() }
    }
}