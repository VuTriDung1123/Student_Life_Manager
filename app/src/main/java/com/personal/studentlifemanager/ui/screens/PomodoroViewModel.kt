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

    var selectedDate by mutableStateOf(Calendar.getInstance())
        private set

    var currentDateRecords by mutableStateOf<List<PomodoroRecord>>(emptyList())
        private set

    var weeklyTotalMinutes by mutableStateOf(0)
        private set

    var currentStreak by mutableStateOf(0)
        private set

    // 🔥 BIẾN MỚI CHO TRANG BÁO CÁO: Lưu TOÀN BỘ lịch sử (Cả Thành công lẫn Thất bại)
    var allTimeRecords by mutableStateOf<List<PomodoroRecord>>(emptyList())
        private set

    init {
        fetchRecordsForSelectedDate()
        fetchWeeklyTotal()
        calculateStreak()
        fetchAllRecordsForReport() // Gọi hàm tải dữ liệu báo cáo
    }

    fun changeDate(offsetDays: Int) {
        val newDate = selectedDate.clone() as Calendar
        newDate.add(Calendar.DAY_OF_YEAR, offsetDays)
        selectedDate = newDate
        fetchRecordsForSelectedDate()
    }

    fun fetchRecordsForSelectedDate() {
        val userId = auth.currentUser?.uid ?: return
        val startOfDay = selectedDate.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0); startOfDay.set(Calendar.MINUTE, 0); startOfDay.set(Calendar.SECOND, 0); startOfDay.set(Calendar.MILLISECOND, 0)
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

    private fun fetchWeeklyTotal() {
        val userId = auth.currentUser?.uid ?: return

        val startOfWeek = Calendar.getInstance()
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0); startOfWeek.set(Calendar.MINUTE, 0); startOfWeek.set(Calendar.SECOND, 0); startOfWeek.set(Calendar.MILLISECOND, 0)

        db.collection("users").document(userId).collection("pomodoro_records")
            .whereGreaterThanOrEqualTo("startTime", startOfWeek.timeInMillis)
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                weeklyTotalMinutes = records.filter { it.isCompleted }.sumOf { it.actualFocusMinutes }
            }
    }

    private fun calculateStreak() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records")
            .whereEqualTo("isCompleted", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                val activeDates = records.map {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                }.toSet()

                var streak = 0
                val checkCal = Calendar.getInstance()
                val todayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"

                if (activeDates.contains(todayKey)) {
                    while (activeDates.contains("${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}")) {
                        streak++; checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    }
                } else {
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"
                    if (activeDates.contains(yesterdayKey)) {
                        while (activeDates.contains("${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}")) {
                            streak++; checkCal.add(Calendar.DAY_OF_YEAR, -1)
                        }
                    }
                }
                currentStreak = streak
            }
    }

    // 🔥 HÀM MỚI: Tải toàn bộ data không phân biệt thành công/thất bại
    private fun fetchAllRecordsForReport() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records")
            .get()
            .addOnSuccessListener { snapshot ->
                allTimeRecords = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
            }
    }

    fun saveRecord(record: PomodoroRecord) {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(userId).collection("pomodoro_records").document()
        record.id = ref.id
        ref.set(record).addOnSuccessListener {
            fetchRecordsForSelectedDate()
            fetchWeeklyTotal()
            calculateStreak()
            fetchAllRecordsForReport() // Cập nhật lại kho data báo cáo
        }
    }

    fun deleteRecord(recordId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records").document(recordId).delete().addOnSuccessListener {
            fetchRecordsForSelectedDate()
            fetchWeeklyTotal()
            calculateStreak()
            fetchAllRecordsForReport() // Cập nhật lại kho data báo cáo
        }
    }
}