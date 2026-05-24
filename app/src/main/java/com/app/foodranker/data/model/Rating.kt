package com.app.foodranker.data.model

data class Rating(
    val id: String = "",
    val plateId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val flavorScore: Float = 0f,      // Sabor (1-10)
    val presentationScore: Float = 0f, // Presentación (1-10)
    val valueScore: Float = 0f,        // Precio/Calidad (1-10)
    val averageScore: Double = 0.0,    // Calculado automáticamente
    val comment: String = "",
    val createdAt: Long = 0L
)