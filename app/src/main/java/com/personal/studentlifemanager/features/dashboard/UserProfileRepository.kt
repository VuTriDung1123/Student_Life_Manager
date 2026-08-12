package com.personal.studentlifemanager.features.dashboard

import com.personal.studentlifemanager.core.database.dao.UserProfileDao
import com.personal.studentlifemanager.core.database.entities.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserProfileRepository(private val userProfileDao: UserProfileDao) {

    fun getUserProfile(uid: String): Flow<UserProfile?> {
        return userProfileDao.getUserProfile(uid).map { it?.toModel() }
    }

    suspend fun saveUserProfile(userProfile: UserProfile) {
        userProfileDao.insertUserProfile(userProfile.toEntity())
    }

    // --- MAPPERS ---
    private fun UserProfileEntity.toModel() = UserProfile(uid, name, level, currentExp, nextLevelExp)
    private fun UserProfile.toEntity() = UserProfileEntity(uid, name, level, currentExp, nextLevelExp)
}
