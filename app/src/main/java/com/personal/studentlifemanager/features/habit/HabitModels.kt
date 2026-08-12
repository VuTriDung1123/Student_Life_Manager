package com.personal.studentlifemanager.features.habit

data class Habit(
    val id: String = "",
    val title: String = "",
    val frequency: String = "DAILY",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val createdAt: Long = 0L
)

data class HabitLog(
    val id: String = "",
    val habitId: String = "",
    val dateMs: Long = 0L,
    val status: String = "COMPLETED" // COMPLETED, SKIPPED
)
