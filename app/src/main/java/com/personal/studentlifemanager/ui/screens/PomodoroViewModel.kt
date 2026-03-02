package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    // 🔥 THÊM BIẾN QUẢN LÝ STREAK NGÀY
    var currentStreak by mutableStateOf(0)
        private set

    init {
        fetchRecordsForSelectedDate()
        fetchWeeklyTotal()
        calculateStreak() // Tính streak ngay khi mở app
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

    // 🔥 TÍNH TỔNG THỜI GIAN TRONG TUẦN (Đã fix lỗi Index của Firestore)
    private fun fetchWeeklyTotal() {
        val userId = auth.currentUser?.uid ?: return

        val startOfWeek = Calendar.getInstance()
        // Đặt mốc về đầu tuần (thường là Thứ 2 hoặc Chủ Nhật tùy máy)
        startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.firstDayOfWeek)
        startOfWeek.set(Calendar.HOUR_OF_DAY, 0)
        startOfWeek.set(Calendar.MINUTE, 0)
        startOfWeek.set(Calendar.SECOND, 0)
        startOfWeek.set(Calendar.MILLISECOND, 0)

        db.collection("users").document(userId).collection("pomodoro_records")
            .whereGreaterThanOrEqualTo("startTime", startOfWeek.timeInMillis)
            // BỎ qua lệnh whereEqualTo ở đây để tránh lỗi đòi Composite Index của Firebase
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }

                // 🔥 Đem về Kotlin lọc và tính tổng thời gian (Chỉ cộng những phiên Thành công)
                weeklyTotalMinutes = records.filter { it.isCompleted }.sumOf { it.actualFocusMinutes }
            }
    }

    // 🔥 THUẬT TOÁN TÍNH CHUỖI NGÀY LIÊN TIẾP (STREAK)
    private fun calculateStreak() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records")
            .whereEqualTo("isCompleted", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }

                // Gom tất cả các ngày có học thành một tập hợp chuỗi (VD: "2026-61" - Ngày thứ 61 của năm 2026)
                // Cách này chống lại mọi sai số về mili-giây hay múi giờ
                val activeDates = records.map {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.startTime }
                    "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
                }.toSet()

                var streak = 0
                val checkCal = Calendar.getInstance() // Mốc kiểm tra bắt đầu từ Hôm nay
                val todayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"

                if (activeDates.contains(todayKey)) {
                    // TRƯỜNG HỢP 1: Hôm nay ĐÃ cày Pomodoro
                    // -> Bắt đầu đếm từ hôm nay, lùi dần về các ngày trước
                    while (activeDates.contains("${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}")) {
                        streak++
                        checkCal.add(Calendar.DAY_OF_YEAR, -1) // Lùi về 1 ngày
                    }
                } else {
                    // TRƯỜNG HỢP 2: Hôm nay CHƯA cày Pomodoro
                    // -> Lùi về hôm qua kiểm tra xem có cày không (Streak được bảo lưu đến hết 23:59 hôm nay)
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                    val yesterdayKey = "${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}"

                    if (activeDates.contains(yesterdayKey)) {
                        while (activeDates.contains("${checkCal.get(Calendar.YEAR)}-${checkCal.get(Calendar.DAY_OF_YEAR)}")) {
                            streak++
                            checkCal.add(Calendar.DAY_OF_YEAR, -1) // Lùi về 1 ngày
                        }
                    }
                }

                currentStreak = streak
            }
    }

    fun saveRecord(record: PomodoroRecord) {
        val userId = auth.currentUser?.uid ?: return
        val ref = db.collection("users").document(userId).collection("pomodoro_records").document()
        record.id = ref.id
        ref.set(record).addOnSuccessListener {
            fetchRecordsForSelectedDate()
            fetchWeeklyTotal()
            calculateStreak() // Cập nhật lại streak
        }
    }

    fun deleteRecord(recordId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records").document(recordId).delete().addOnSuccessListener {
            fetchRecordsForSelectedDate()
            fetchWeeklyTotal()
            calculateStreak()
        }
    }


    // ==========================================
    // 🔥 CHIẾN DỊCH 2: LOCAL ANALYTICS
    // ==========================================
    var averageFocusLength by mutableIntStateOf(0)
        private set
    var bestTimeOfDay by mutableStateOf("Chưa có")
        private set
    var mostProductiveDay by mutableStateOf("Chưa có")
        private set

    // 🔥 BIẾN MỚI: Thống kê Task toàn thời gian
    var allTimeTaskStats by mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap())
        private set

    // Đừng quên gọi fetchAnalyticsData() trong khối init{} và trong hàm saveRecord(), deleteRecord() nhé!
    private fun fetchAnalyticsData() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records")
            .whereEqualTo("isCompleted", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val records = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                if (records.isEmpty()) {
                    allTimeTaskStats = emptyMap()
                    return@addOnSuccessListener
                }

                // 1. Thời lượng trung bình
                averageFocusLength = records.sumOf { it.actualFocusMinutes } / records.size

                // 2. Khung giờ vàng
                val timeGroup = records.groupBy {
                    val hour = Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.HOUR_OF_DAY)
                    when (hour) { in 5..11 -> "Buổi Sáng"; in 12..17 -> "Buổi Chiều"; else -> "Buổi Tối" }
                }
                bestTimeOfDay = timeGroup.maxByOrNull { it.value.size }?.key ?: "Chưa có"

                // 3. Ngày năng suất nhất
                val dayGroup = records.groupBy {
                    val day = Calendar.getInstance().apply { timeInMillis = it.startTime }.get(Calendar.DAY_OF_WEEK)
                    when (day) {
                        Calendar.MONDAY -> "Thứ 2"; Calendar.TUESDAY -> "Thứ 3"; Calendar.WEDNESDAY -> "Thứ 4"
                        Calendar.THURSDAY -> "Thứ 5"; Calendar.FRIDAY -> "Thứ 6"; Calendar.SATURDAY -> "Thứ 7"; else -> "Chủ Nhật"
                    }
                }
                mostProductiveDay = dayGroup.maxByOrNull { it.value.sumOf { r -> r.actualFocusMinutes } }?.key ?: "Chưa có"

                // 🔥 4. THỐNG KÊ TASK (Gom nhóm theo tên Task, trả về Pair<Số phiên, Tổng phút>)
                val taskGroup = records.groupBy { if (it.taskName.isBlank()) "Tự do" else it.taskName }
                allTimeTaskStats = taskGroup.mapValues { entry ->
                    Pair(entry.value.size, entry.value.sumOf { it.actualFocusMinutes })
                }
            }
    }
}