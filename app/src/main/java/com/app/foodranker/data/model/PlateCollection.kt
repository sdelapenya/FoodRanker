package com.app.foodranker.data.model

data class PlateCollection(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val emoji: String = "🍽️",
    val plateIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
