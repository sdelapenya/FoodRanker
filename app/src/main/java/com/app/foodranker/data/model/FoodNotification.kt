package com.app.foodranker.data.model

data class FoodNotification(
    val id: String = "",
    // "like" | "rating" | "comment" | "moderation_approved" | "moderation_rejected"
    val type: String = "",
    val fromUserName: String = "",
    val plateId: String = "",
    val plateName: String = "",
    val score: Double = 0.0,        // solo para tipo "rating"
    val commentText: String = "",   // solo para tipo "comment"
    val reasons: List<String> = emptyList(), // solo para "moderation_rejected"
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)
