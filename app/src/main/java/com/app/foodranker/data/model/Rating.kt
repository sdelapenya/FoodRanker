package com.app.foodranker.data.model

data class Rating(
    val id: String = "",
    val plateId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhotoUrl: String = "",
    val flavorScore: Float = 0f,      // Sabor (1-10)
    val presentationScore: Float = 0f, // Presentación (1-10)
    /** ¿Te quedas satisfecho? 1 = con hambre, 10 = perfectamente. Null antes del rediseño. */
    val satisfactionScore: Float? = null,
    /** Precio/Calidad (1-10) — eje retirado. Se conserva para el histórico, ya no se escribe. */
    val valueScore: Float = 0f,
    /** Null en valoraciones anteriores al rediseño. */
    val wouldOrderAgain: Boolean? = null,
    /** Precio pagado, en céntimos. Null si no se aportó. */
    val pricePaidCents: Int? = null,
    /** El GPS coincidía con el local al valorar — distintivo, no pondera nada. */
    val verifiedAtVenue: Boolean = false,
    val averageScore: Double = 0.0,    // Calculado automáticamente
    val comment: String = "",
    val createdAt: Long = 0L
) {
    companion object {
        const val WEIGHT_FLAVOR = 0.50
        const val WEIGHT_PRESENTATION = 0.20
        const val WEIGHT_SATISFACTION = 0.30

        /** Tope de cordura del precio (1.000 €). Debe reflejar `priceOk` en firestore.rules. */
        const val MAX_PRICE_CENTS = 100_000

        /**
         * A cuántos metros del local se considera que el voto se emitió allí. 200 m cubre el
         * error de GPS urbano más el tamaño del local sin abrir la mano de más.
         * Ver docs/RATINGS.md §5.
         */
        const val VENUE_RADIUS_METERS = 200.0

        /** "12,50" o "12.50" → 1250 céntimos. Null si está vacío o fuera de rango. */
        fun parsePriceToCents(text: String): Int? {
            val normalized = text.trim().replace(',', '.')
            if (normalized.isEmpty()) return null
            val euros = normalized.toDoubleOrNull() ?: return null
            val cents = Math.round(euros * 100).toInt()
            return cents.takeIf { it in 1..MAX_PRICE_CENTS }
        }

        fun formatPrice(cents: Int?): String =
            if (cents == null || cents <= 0) "" else "%.2f".format(cents / 100.0)

        /**
         * Nota ponderada de una valoración. El servidor la recalcula con esta misma fórmula en
         * `onRatingCreated`/`onRatingUpdated` y nunca se fía de la que manda el cliente.
         *
         * Sin [satisfactionScore] (valoraciones anteriores al rediseño) se renormaliza sobre los
         * ejes disponibles en vez de suponer un valor: `valueScore` medía precio/calidad, no
         * saciedad, y reutilizarlo falsearía el dato. Ver docs/RATINGS.md §3.
         */
        fun computeAverage(
            flavorScore: Float,
            presentationScore: Float,
            satisfactionScore: Float?
        ): Double {
            val safeFlavor = flavorScore.coerceIn(1f, 10f)
            val safePresentation = presentationScore.coerceIn(1f, 10f)
            val weighted = WEIGHT_FLAVOR * safeFlavor + WEIGHT_PRESENTATION * safePresentation
            if (satisfactionScore == null) {
                return weighted / (WEIGHT_FLAVOR + WEIGHT_PRESENTATION)
            }
            return weighted + WEIGHT_SATISFACTION * satisfactionScore.coerceIn(1f, 10f)
        }
    }
}
