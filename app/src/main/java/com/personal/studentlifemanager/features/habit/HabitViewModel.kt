package com.personal.studentlifemanager.features.habit

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.studentlifemanager.core.database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = HabitRepository(AppDatabase.getDatabase(application).habitDao())

    var habits by mutableStateOf<List<Habit>>(emptyList())
        private set

    var habitLogs by mutableStateOf<List<HabitLog>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            repository.getHabits().collectLatest { list ->
                habits = list
            }
        }
        
        // Fetch logs for the current week or month for heatmap
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1) // Fetch last 30 days roughly
        val startDate = cal.timeInMillis
        val endDate = System.currentTimeMillis()
        
        viewModelScope.launch {
            repository.getLogsInDateRange(startDate, endDate).collectLatest { logs ->
                habitLogs = logs
            }
        }
    }

    fun addHabit(title: String, frequency: String, onSuccess: () -> Unit) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addHabit(title, frequency)
            onSuccess()
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun checkIn(habit: Habit) {
        viewModelScope.launch {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Avoid duplicate check-ins for today
            val alreadyCheckedIn = habitLogs.any { it.habitId == habit.id && it.dateMs == today && it.status == "COMPLETED" }
            if (alreadyCheckedIn) return@launch

            repository.addHabitLog(habit.id, today, "COMPLETED")

            val newStreak = habit.currentStreak + 1
            val newBestStreak = if (newStreak > habit.bestStreak) newStreak else habit.bestStreak
            
            repository.updateHabit(habit.copy(currentStreak = newStreak, bestStreak = newBestStreak))
            
            // TODO: Triggers gamification EXP reward
            // userProfileViewModel.addExp(10)
        }
    }

    fun getLogsForHabit(habitId: String): List<HabitLog> {
        return habitLogs.filter { it.habitId == habitId }
    }
}
