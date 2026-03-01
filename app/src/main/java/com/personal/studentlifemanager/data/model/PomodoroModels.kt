package com.personal.studentlifemanager.data.model

import com.google.firebase.firestore.PropertyName

// Lưu trữ cấu hình thời gian người dùng thiết lập
data class PomodoroConfig(
    val focusTime: Int = 25,     // Phút tập trung
    val shortBreak: Int = 5,     // Phút nghỉ ngắn
    val sessionsCount: Int = 4,  // Số phiên trước khi nghỉ dài
    val longBreak: Int = 15      // Phút nghỉ dài
)

// Lưu trữ lịch sử 1 phiên tập trung (Để biết Hoàn thành hay Thất bại)
// 🔥 BẢN NÂNG CẤP: Lịch sử chi tiết của 1 phiên Pomodoro
data class PomodoroRecord(
    var id: String = "",
    var startTime: Long = 0L,         // Thời điểm bấm Bắt đầu
    var endTime: Long = 0L,           // Thời điểm kết thúc/hủy

    // Cấu hình lúc chạy (Lỡ sau này user đổi cấu hình thì lịch sử cũ không bị sai)
    var configFocus: Int = 0,
    var configShort: Int = 0,
    var configSessions: Int = 0,
    var configLong: Int = 0,

    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false, // true = Thành công, false = Thất bại

    var actualFocusMinutes: Int = 0,
    var taskName: String = ""

)