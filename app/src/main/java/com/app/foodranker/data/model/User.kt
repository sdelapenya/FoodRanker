package com.app.foodranker.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val bio: String = "",
    val city: String = "",
    val website: String = "",
    val totalPlatesAdded: Int = 0,
    val totalRatings: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    val isPremium: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    val badges: List<String> = emptyList(),
    val referralCount: Int = 0,
    val referredByUserId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
