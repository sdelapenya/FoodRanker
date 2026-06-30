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
) {
    companion object {
        /** Clampa los 3 sub-scores a [1,10] y devuelve su media — misma fórmula en todas las pantallas que envían una valoración. */
        fun computeAverage(flavorScore: Float, presentationScore: Float, valueScore: Float): Double {
            val safeFlavor = flavorScore.coerceIn(1f, 10f)
            val safePresentation = presentationScore.coerceIn(1f, 10f)
            val safeValue = valueScore.coerceIn(1f, 10f)
            return (safeFlavor + safePresentation + safeValue) / 3.0
        }
    }
}