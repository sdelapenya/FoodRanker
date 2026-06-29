package com.app.foodranker.utils

object RewardManager {

    // ── XP por acción ─────────────────────────────────────────────
    const val XP_PLATE_WITH_PHOTO = 50
    const val XP_PLATE_NO_PHOTO   = 10
    const val XP_GIVE_RATING      = 5
    const val XP_RECEIVE_RATING   = 10
    const val XP_RECEIVE_LIKE     = 3
    const val XP_REFERRAL         = 100

    // ── Definición de niveles ──────────────────────────────────────
    // Umbrales deben coincidir con getLevel() en functions/src/index.ts.
    // El cliente siempre recalcula el nivel a partir de xp (no lee el campo
    // `level` que guarda la Cloud Function); si los umbrales divergen, ese
    // campo dejaría de ser coherente con lo que ve el usuario aquí.
    data class Level(
        val number: Int,
        val emoji: String,
        val name: String,
        val minXP: Int,
        val maxXP: Int
    )

    val LEVELS = listOf(
        Level(1, "🥄", "Novato Foodie",          0,     199),
        Level(2, "🍴", "Explorador",              200,   599),
        Level(3, "👨‍🍳", "Crítico Gastronómico",   600,  1499),
        Level(4, "🌟", "Gourmand",               1500,  3999),
        Level(5, "🏆", "Top Chef",               4000,  9999),
        Level(6, "💎", "Leyenda Foodie",        10000, Int.MAX_VALUE)
    )

    fun getLevel(xp: Int): Level = LEVELS.lastOrNull { xp >= it.minXP } ?: LEVELS.first()

    fun getProgress(xp: Int): Float {
        val level = getLevel(xp)
        if (level.number == 6) return 1f
        val range = (level.maxXP - level.minXP + 1).toFloat()
        return ((xp - level.minXP) / range).coerceIn(0f, 1f)
    }

    fun getNextLevelXP(xp: Int): Int {
        val level = getLevel(xp)
        return if (level.number == 6) xp else level.maxXP + 1
    }

    // ── Definición de badges ───────────────────────────────────────
    data class Badge(val id: String, val emoji: String, val name: String, val description: String)

    val ALL_BADGES = listOf(
        Badge("first_plate",  "📸", "Primera foto",      "Subiste tu primer plato"),
        Badge("globetrotter", "🌍", "Globetrotter",      "Platos en 3 países distintos"),
        Badge("popular",      "❤️", "Popular",           "50 likes recibidos"),
        Badge("critic",       "⭐", "Crítico",           "10 valoraciones dadas"),
        Badge("top10",        "🏆", "Top 10",            "Un plato tuyo en el Top 10")
    )

    fun getBadge(id: String): Badge? = ALL_BADGES.find { it.id == id }
    // XP and badge writes are handled server-side by Cloud Functions (onRatingCreated, moderatePlateImage)
}
