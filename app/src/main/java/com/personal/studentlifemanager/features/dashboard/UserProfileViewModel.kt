package com.personal.studentlifemanager.features.dashboard

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.personal.studentlifemanager.core.database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserProfileRepository(AppDatabase.getDatabase(application).userProfileDao())
    
    // For local gamification, we can use a hardcoded UID or get it from FirebaseAuth
    // To keep it offline-first, let's use a default UID "local_user"
    private val uid = "local_user"

    var userProfile by mutableStateOf(UserProfile(uid = uid))
        private set

    init {
        viewModelScope.launch {
            repository.getUserProfile(uid).collectLatest { profile ->
                if (profile != null) {
                    userProfile = profile
                } else {
                    // Create default profile
                    repository.saveUserProfile(UserProfile(uid = uid))
                }
            }
        }
    }

    fun addExp(amount: Int) {
        viewModelScope.launch {
            var current = userProfile.currentExp + amount
            var level = userProfile.level
            var nextLevelExp = userProfile.nextLevelExp

            while (current >= nextLevelExp) {
                current -= nextLevelExp
                level += 1
                nextLevelExp = (nextLevelExp * 1.5).toInt() // Required EXP grows
            }

            val newProfile = userProfile.copy(
                level = level,
                currentExp = current,
                nextLevelExp = nextLevelExp
            )
            repository.saveUserProfile(newProfile)
        }
    }
}
