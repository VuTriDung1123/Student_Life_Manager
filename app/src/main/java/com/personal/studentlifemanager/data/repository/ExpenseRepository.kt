package com.personal.studentlifemanager.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.personal.studentlifemanager.data.model.Transaction

class ExpenseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    init {
        // ĐIỂM ĂN TIỀN CV Ở ĐÂY: Bật tính năng Offline Persistence (Lưu trữ ngoại tuyến)
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder().build()
            )
            .build()
        firestore.firestoreSettings = settings
    }

    // Lấy ID của user đang đăng nhập (để dữ liệu ai người nấy xem)
    private val userId get() = auth.currentUser?.uid ?: "unknown_user"

    // Tham chiếu đến bảng "transactions" của user này
    private val transactionRef get() = firestore
        .collection("users")
        .document(userId)
        .collection("transactions")

    // Hàm 1: Thêm giao dịch mới
    fun addTransaction(transaction: Transaction, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        // Tạo một ID ngẫu nhiên cho Document
        val docRef = transactionRef.document()
        val transactionWithId = transaction.copy(id = docRef.id)

        docRef.set(transactionWithId)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    // Hàm 2: Lấy danh sách giao dịch (Tự động cập nhật real-time)
    fun getTransactions(onResult: (List<Transaction>) -> Unit) {
        transactionRef.orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { it.toObject(Transaction::class.java) } ?: emptyList()
                onResult(list)
            }
    }
}