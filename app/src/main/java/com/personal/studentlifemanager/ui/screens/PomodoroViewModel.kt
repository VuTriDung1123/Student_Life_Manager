package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore // Nếu bạn dùng Realtime Database thì báo mình đổi nhé, ở đây mình dùng Firestore chuẩn mới
import com.google.firebase.firestore.Query
import com.personal.studentlifemanager.data.model.PomodoroRecord
import java.util.Calendar

class PomodoroViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Biến chứa danh sách lịch sử hôm nay để UI tự động cập nhật
    var todayRecords by mutableStateOf<List<PomodoroRecord>>(emptyList())
        private set

    init {
        fetchTodayRecords()
    }

    // 1. LƯU LỊCH SỬ LÊN FIREBASE
    fun saveRecord(record: PomodoroRecord) {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(userId).collection("pomodoro_records").document()

        record.id = ref.id
        ref.set(record).addOnSuccessListener {
            fetchTodayRecords() // Lưu xong thì tải lại danh sách luôn
        }
    }

    // 2. LẤY LỊCH SỬ CỦA NGÀY HÔM NAY
    fun fetchTodayRecords() {
        val userId = auth.currentUser?.uid ?: return

        // Tính thời điểm 00:00:00 của ngày hôm nay
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis

        db.collection("users").document(userId).collection("pomodoro_records")
            .whereGreaterThanOrEqualTo("startTime", startOfDay)
            .orderBy("startTime", Query.Direction.DESCENDING) // Xếp mới nhất lên đầu
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                todayRecords = records
            }
    }
}