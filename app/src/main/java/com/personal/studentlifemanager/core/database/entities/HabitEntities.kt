package com.personal.studentlifemanager.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val frequency: String, // "DAILY", "WEEKLY", vv.
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("habitId"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index(value = ["habitId"])]
)
data class HabitLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val dateMs: Long,
    val status: String // "COMPLETED", "SKIPPED"
)
