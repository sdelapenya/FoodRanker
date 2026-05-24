package com.app.foodranker.data.model

data class FoodNotification(
    val id: String = "",
    val type: String = "",          // "like" | "rating" | "moderation_rejected"
    val fromUserName: String = "",
    val plateId: String = "",
    val plateName: String = "",
    val score: Double = 0.0,        // solo para tipo "rating"
    val reasons: List<String> = emptyList(), // solo para "moderation_rejected"
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)
