package com.personal.studentlifemanager.features.dashboard

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val level: Int = 1,
    val currentExp: Int = 0,
    val nextLevelExp: Int = 100
)
