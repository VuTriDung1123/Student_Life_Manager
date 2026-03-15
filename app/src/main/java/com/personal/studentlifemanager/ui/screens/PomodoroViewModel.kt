package com.personal.studentlifemanager.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.personal.studentlifemanager.data.model.PomodoroConfig
import com.personal.studentlifemanager.data.model.PomodoroRecord
import java.util.Calendar

class PomodoroViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var selectedDate by mutableStateOf(Calendar.getInstance())
        private set

    // 🔥 ÉP KIỂU RÕ RÀNG BẰNG LIST ĐỂ KOTLIN KHÔNG BỊ BỐI RỐI
    var currentDateRecords by mutableStateOf<List<PomodoroRecord>>(emptyList())
        private set

    var weeklyTotalMinutes by mutableStateOf(0)
        private set

    var currentStreak by mutableStateOf(0)
        private set

    var allTimeRecords by mutableStateOf<List<PomodoroRecord>>(emptyList())
        private set

    // ==========================================
    // 🔥 CHIẾN DỊCH 5: DAILY PRODUCTIVITY SCORE
    // ==========================================
    var dailyGoalMinutes by mutableIntStateOf(120)
        private set
    var productivityScore by mutableIntStateOf(0)
        private set

    // ==========================================
    // 🔥 CHIẾN DỊCH 3: GAMIFICATION (THÀNH TỰU & NHIỆM VỤ TUẦN)
    // ==========================================
    data class PomodoroBadge(val id: Int, val name: String, val desc: String, val icon: String, var isUnlocked: Boolean = false)
    data class WeeklyMission(val id: Int, val title: String, val desc: String, val target: Int, val isMinutes: Boolean)

    private val weeklyMissionsList = listOf(
        WeeklyMission(0, "Khởi Động Nhẹ Nhàng", "Hoàn thành 10 phiên Pomodoro", 10, false),
        WeeklyMission(1, "Sức Bền Thời Gian", "Tích lũy 200 phút tập trung", 200, true),
        WeeklyMission(2, "Chiến Binh Chăm Chỉ", "Hoàn thành 20 phiên Pomodoro", 20, false),
        WeeklyMission(3, "Thợ Săn Cà Chua", "Tích lũy 400 phút tập trung", 400, true),
        WeeklyMission(4, "Bứt Phá Giới Hạn", "Hoàn thành 25 phiên Pomodoro", 25, false),
        WeeklyMission(5, "Tuần Lễ Tập Trung", "Tích lũy 600 phút tập trung", 600, true),
        WeeklyMission(6, "Duy Trì Phong Độ", "Hoàn thành 15 phiên Pomodoro", 15, false),
        WeeklyMission(7, "Năng Suất Tối Đa", "Tích lũy 500 phút tập trung", 500, true),
        WeeklyMission(8, "Cỗ Máy Thời Gian", "Hoàn thành 30 phiên Pomodoro", 30, false),
        WeeklyMission(9, "Thử Thách Cực Đại", "Tích lũy 800 phút tập trung", 800, true)
    )

    var currentWeeklyMission by mutableStateOf(weeklyMissionsList[0])
        private set
    var currentWeeklyProgress by mutableIntStateOf(0)
        private set
    var unlockedBadges by mutableStateOf<List<PomodoroBadge>>(emptyList())
        private set


    init {
        fetchRecordsForSelectedDate()
        fetchWeeklyTotal()
        calculateStreak()
        fetchAllRecordsForReport()
    }

    fun changeDate(offsetDays: Int) {
        val newDate = selectedDate.clone() as Calendar
        newDate.add(Calendar.DAY_OF_YEAR, offsetDays)
        selectedDate = newDate
        fetchRecordsForSelectedDate()
    }

    // 🔥 HÀM NÀY ĐÃ ĐƯỢC CHỮA SẠCH LỖI VÀ ÉP KIỂU CHUẨN MỰC
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
                // Ép kiểu tường minh ra List để tránh lỗi Ambiguity
                val records: List<PomodoroRecord> = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                currentDateRecords = records

                // Tính điểm
                if (records.isEmpty()) {
                    productivityScore = 0
                } else {
                    // Dùng biến records đã được ép kiểu List
                    val successRecords = records.filter { it.isCompleted == true }
                    val totalMins = successRecords.sumOf { it.actualFocusMinutes ?: 0 }
                    val successRate = (successRecords.size.toFloat() / records.size.toFloat())

                    val timeScore = (totalMins.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f) * 60f
                    val disciplineScore = successRate * 40f

                    productivityScore = (timeScore + disciplineScore).toInt()
                }
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
                val records: List<PomodoroRecord> = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                weeklyTotalMinutes = records.filter { it.isCompleted == true }.sumOf { it.actualFocusMinutes ?: 0 }
            }
    }

    private fun calculateStreak() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records")
            .whereEqualTo("isCompleted", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val records: List<PomodoroRecord> = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                val activeDates = records.map {
                    val cal = Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0L }
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

    private fun fetchAllRecordsForReport() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records")
            .get()
            .addOnSuccessListener { snapshot ->
                allTimeRecords = snapshot.documents.mapNotNull { it.toObject(PomodoroRecord::class.java) }
                calculateGamification()
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
            fetchAllRecordsForReport()
        }
    }

    fun deleteRecord(recordId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("pomodoro_records").document(recordId).delete().addOnSuccessListener {
            fetchRecordsForSelectedDate()
            fetchWeeklyTotal()
            calculateStreak()
            fetchAllRecordsForReport()
        }
    }

    private fun calculateGamification() {
        val records: List<PomodoroRecord> = allTimeRecords
        val successRecords = records.filter { it.isCompleted == true }
        val failRecords = records.filter { it.isCompleted == false }

        val totalSuccess = successRecords.size
        val totalMins = successRecords.sumOf { it.actualFocusMinutes ?: 0 }

        val weekOfYear = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        currentWeeklyMission = weeklyMissionsList[weekOfYear % weeklyMissionsList.size]

        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val recordsThisWeek = successRecords.filter { (it.startTime ?: 0L) >= startOfWeek.timeInMillis }

        currentWeeklyProgress = if (currentWeeklyMission.isMinutes) {
            recordsThisWeek.sumOf { it.actualFocusMinutes ?: 0 }
        } else {
            recordsThisWeek.size
        }

        val hasNightOwl = successRecords.any { Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0L }.get(Calendar.HOUR_OF_DAY) in 0..3 }
        val hasEarlyBird = successRecords.any { Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0L }.get(Calendar.HOUR_OF_DAY) in 4..6 }
        val hasNoon = successRecords.any { Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0L }.get(Calendar.HOUR_OF_DAY) in 11..13 }

        val maxSessionsInOneDay = successRecords.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0L }
            "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
        }.maxOfOrNull { it.value.size } ?: 0

        val hasLongFocus = successRecords.any { (it.actualFocusMinutes ?: 0) >= 50 }
        val hasSpeedFocus = successRecords.any { (it.actualFocusMinutes ?: 0) in 1..15 }
        val weekendSessions = successRecords.count {
            val day = Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0L }.get(Calendar.DAY_OF_WEEK)
            day == Calendar.SATURDAY || day == Calendar.SUNDAY
        }
        val namedTasksCount = successRecords.count { (it.taskName ?: "").isNotBlank() && it.taskName != "Tự do" }

        val badges = mutableListOf(
            PomodoroBadge(1, "Tân Binh", "Hoàn thành phiên đầu tiên", "🌱", totalSuccess >= 1),
            PomodoroBadge(2, "Thợ Săn", "Hoàn thành 10 phiên", "🏹", totalSuccess >= 10),
            PomodoroBadge(3, "Tinh Anh", "Hoàn thành 50 phiên", "⚔️", totalSuccess >= 50),
            PomodoroBadge(4, "Huyền Thoại", "Hoàn thành 100 phiên", "👑", totalSuccess >= 100),
            PomodoroBadge(5, "Thần Thoại", "Hoàn thành 500 phiên", "🐉", totalSuccess >= 500),

            PomodoroBadge(6, "Cú Đêm", "Học từ 0h - 4h sáng", "🦉", hasNightOwl),
            PomodoroBadge(7, "Bình Minh", "Học từ 4h - 6h sáng", "🌅", hasEarlyBird),
            PomodoroBadge(8, "Xuyên Trưa", "Học từ 11h - 13h trưa", "☀️", hasNoon),

            PomodoroBadge(9, "Combo x4", "4 phiên trong 1 ngày", "🔥", maxSessionsInOneDay >= 4),
            PomodoroBadge(10, "Combo Bạo Kích", "8 phiên trong 1 ngày", "💥", maxSessionsInOneDay >= 8),

            PomodoroBadge(11, "Lửa Nhỏ", "Streak 3 ngày liên tiếp", "🕯️", currentStreak >= 3),
            PomodoroBadge(12, "Lửa Thiêng", "Streak 7 ngày liên tiếp", "🏕️", currentStreak >= 7),
            PomodoroBadge(13, "Bất Diệt", "Streak 30 ngày liên tiếp", "🌋", currentStreak >= 30),

            PomodoroBadge(14, "Tuần Lễ Vàng", "Hoàn thành Nhiệm vụ Tuần này", "🏆", currentWeeklyProgress >= currentWeeklyMission.target),

            PomodoroBadge(15, "Nhịp Điệu Chậm", "1 phiên dài >= 50 phút", "🎵", hasLongFocus),
            PomodoroBadge(16, "Tốc Độ Ánh Sáng", "1 phiên chớp nhoáng <= 15 phút", "⚡", hasSpeedFocus),

            PomodoroBadge(17, "Khối Sinh Tồn", "Cày 10 phiên vào Thứ 7/CN", "🔲", weekendSessions >= 10),
            PomodoroBadge(18, "Nhà Lên Kế Hoạch", "Ghi tên Task cho 20 phiên", "📝", namedTasksCount >= 20),
            PomodoroBadge(19, "Không Bỏ Cuộc", "Có thất bại nhưng vẫn đạt 10 thành công", "❤️‍🩹", failRecords.isNotEmpty() && totalSuccess >= 10),

            PomodoroBadge(20, "Gacha May Mắn", "Đạt tổng số 777 phút", "🎰", totalMins >= 777),
            PomodoroBadge(21, "Cỗ Máy Thời Gian", "Tích lũy 1000 phút", "⏳", totalMins >= 1000),
            PomodoroBadge(22, "Siêu Việt", "Tích lũy 5000 phút", "🌌", totalMins >= 5000)
        )

        val unlockedCount = badges.count { it.isUnlocked }
        badges.add(PomodoroBadge(23, "Kẻ Thống Trị Vạn Vật", "Mở khóa 22 huy hiệu trên", "👁️‍🗨️", unlockedCount == 22))

        unlockedBadges = badges
    }
}