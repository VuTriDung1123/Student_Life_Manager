package com.personal.studentlifemanager.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val level: Int = 1,
    val currentExp: Int = 0,
    val nextLevelExp: Int = 100
)
