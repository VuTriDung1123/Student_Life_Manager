package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.personal.studentlifemanager.data.model.PomodoroRecord
import java.util.Calendar

class PomodoroViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // 1. Quản lý ngày đang xem (Mặc định là hôm nay)
    var selectedDate by mutableStateOf(Calendar.getInstance())
        private set

    // 2. Lịch sử của ngày được chọn
    var currentDateRecords by mutableStateOf<List<PomodoroRecord>>(emptyList())
        private set

    // 3. Tổng thời gian focus trong tuần này
    var weeklyTotalMinutes by mutableStateOf(0)
        private set

    init {
        fetchRecordsForSelectedDate()
        fetchWeeklyTotal()
    }

    // 🔥 HÀM CHUYỂN NGÀY (VD: offset = -1 là lùi về hôm qua)
    fun changeDate(offsetDays: Int) {
        val newDate = selectedDate.clone() as Calendar
        newDate.add(Calendar.DAY_OF_YEAR, offsetDays)
        selectedDate = newDate
        fetchRecordsForSelectedDate()
    }

    // 🔥 LỌC LỊCH SỬ THEO NGÀY ĐANG CHỌN
    fun fetchRecordsForSelectedDate() {
        val userId = auth.currentUser?.uid ?: return

        // Tính mốc 00:00:00 của ngày được chọn
        val startOfDay = selectedDate.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        // Tính mốc 23:59:59 của ngày được chọn
        val endOfDay = startOfDay.clone() as Calendar
        endOfDay.add(Calendar.DAY_OF_YEAR, 1)

        db.collection("users").document(userId).collection("pomodoro_records")
            .whereGreaterThanOrEqualTo("startTime", startOfDay.timeInMillis)
            .whereLessThan("startTime", endOfDay.timeInMillis)
            .orderBy("startTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                currentDateRecords = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
            }
    }

    // 🔥 TÍNH TỔNG THỜI GIAN TRONG TUẦN (Từ Thứ 2 đến Chủ Nhật)
    private fun fetchWeeklyTotal() {
        val userId = auth.currentUser?.uid ?: return

        val startOfWeek = Calendar.getInstance()
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek) // Tùy region, thường là CN hoặc T2
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        startOfWeek.set(Calendar.MINUTE, 0)
        startOfWeek.set(Calendar.SECOND, 0)

        db.collection("users").document(userId).collection("pomodoro_records")
            .whereGreaterThanOrEqualTo("startTime", startOfWeek.timeInMillis)
            .whereEqualTo("isCompleted", true) // Chỉ tính phiên thành công
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                weeklyTotalMinutes = records.sumOf { it.actualFocusMinutes }
            }
    }

    fun saveRecord(record: PomodoroRecord) {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(userId).collection("pomodoro_records").document()
        record.id = ref.id
        ref.set(record).addOnSuccessListener {
            fetchRecordsForSelectedDate()
            fetchWeeklyTotal()
        }
    }

    fun deleteRecord(recordId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records")
            .document(recordId)
            .delete()
            .addOnSuccessListener {
                fetchRecordsForSelectedDate()
                fetchWeeklyTotal()
            }
    }
}