package com.app.foodranker.data.model

data class FoodNotification(
    val id: String = "",
    // "like" | "rating" | "comment" | "follow" | "moderation_approved" | "moderation_rejected"
    val type: String = "",
    val fromUserId: String = "",  // para abrir el perfil en las de tipo "follow"
    val fromUserName: String = "",
    val plateId: String = "",
    val plateName: String = "",
    val score: Double = 0.0,        // solo para tipo "rating"
    val commentText: String = "",   // solo para tipo "comment"
    val position: Int = 0,          // solo para tipo "league_result": el puesto del podio
    val level: Int = 0,             // solo para tipo "level_up"
    val badgeId: String = "",       // solo para tipo "badge"
    val plateCount: Int = 0,        // solo para "new_plate": cuántos lleva ese día
    val reasons: List<String> = emptyList(), // solo para "moderation_rejected"
    val isRead: Boolean = false,
    val createdAt: Long = 0L
)
