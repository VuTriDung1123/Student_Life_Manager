package com.personal.studentlifemanager.features.habit

import com.personal.studentlifemanager.core.database.dao.HabitDao
import com.personal.studentlifemanager.core.database.entities.HabitEntity
import com.personal.studentlifemanager.core.database.entities.HabitLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class HabitRepository(private val habitDao: HabitDao) {

    fun getHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addHabit(title: String, frequency: String) {
        val entity = HabitEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            frequency = frequency,
            createdAt = System.currentTimeMillis()
        )
        habitDao.insertHabit(entity)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit.toEntity())
    }

    suspend fun deleteHabit(habitId: String) {
        habitDao.deleteHabit(HabitEntity(id = habitId, title = "", frequency = ""))
    }

    fun getHabitLogs(habitId: String): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabit(habitId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getLogsInDateRange(startDate: Long, endDate: Long): Flow<List<HabitLog>> {
        return habitDao.getLogsInDateRange(startDate, endDate).map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addHabitLog(habitId: String, dateMs: Long, status: String) {
        val entity = HabitLogEntity(
            id = UUID.randomUUID().toString(),
            habitId = habitId,
            dateMs = dateMs,
            status = status
        )
        habitDao.insertHabitLog(entity)
    }

    // --- MAPPERS ---
    private fun HabitEntity.toModel() = Habit(id, title, frequency, currentStreak, bestStreak, createdAt)
    private fun Habit.toEntity() = HabitEntity(id, title, frequency, currentStreak, bestStreak, createdAt)

    private fun HabitLogEntity.toModel() = HabitLog(id, habitId, dateMs, status)
    private fun HabitLog.toEntity() = HabitLogEntity(id, habitId, dateMs, status)
}
