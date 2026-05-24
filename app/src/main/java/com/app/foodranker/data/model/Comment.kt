package com.app.foodranker.data.model

data class Comment(
    val id: String = "",
    val plateId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)
