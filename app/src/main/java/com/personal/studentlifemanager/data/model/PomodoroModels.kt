package com.personal.studentlifemanager.data.model

// Lưu trữ cấu hình thời gian người dùng thiết lập
data class PomodoroConfig(
    val focusTime: Int = 25,     // Phút tập trung
    val shortBreak: Int = 5,     // Phút nghỉ ngắn
    val sessionsCount: Int = 4,  // Số phiên trước khi nghỉ dài
    val longBreak: Int = 15      // Phút nghỉ dài
)

// Lưu trữ lịch sử 1 phiên tập trung (Để biết Hoàn thành hay Thất bại)
data class PomodoroRecord(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startTime: Long = System.currentTimeMillis(),
    val durationMinutes: Int,    // Độ dài dự kiến (VD: 25 phút)
    val isCompleted: Boolean,    // true = Hoàn thành, false = Thất bại (Hủy giữa chừng)
    val date: Long = System.currentTimeMillis()
)