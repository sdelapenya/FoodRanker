package com.app.foodranker.data.model

data class Report(
    val id: String = "",
    val plateId: String = "",
    val reportedByUserId: String = "",
    val reason: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
