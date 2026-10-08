package com.app.foodranker.utils

import androidx.annotation.StringRes
import com.app.foodranker.R

object RewardManager {

    // ── XP por acción ─────────────────────────────────────────────
    // Deben reflejar las constantes de functions/src/index.ts, que es quien las concede.
    // Publicar vale más que valorar porque cuesta más (foto, local, datos), pero ya no once
    // veces más: eso empujaba a llenar el ranking de platos que nadie llega a votar.
    const val XP_PLATE_WITH_PHOTO = 30
    const val XP_PLATE_NO_PHOTO   = 10
    const val XP_GIVE_RATING      = 15
    const val XP_RECEIVE_RATING   = 10
    const val XP_REFERRAL         = 100

    // ── Definición de niveles ──────────────────────────────────────
    // Umbrales deben coincidir con getLevel() en functions/src/index.ts.
    // El cliente siempre recalcula el nivel a partir de xp (no lee el campo
    // `level` que guarda la Cloud Function); si los umbrales divergen, ese
    // campo dejaría de ser coherente con lo que ve el usuario aquí.
    /**
     * El nombre va como referencia a recurso: la lista se construye al cargar la clase, antes
     * de saber en que idioma esta el movil, asi que un String se quedaria fijo en castellano.
     */
    data class Level(
        val number: Int,
        val emoji: String,
        @StringRes val nameRes: Int,
        val minXP: Int,
        val maxXP: Int
    )

    val LEVELS = listOf(
        Level(1, "🥄", R.string.lvl_1,          0,     199),
        Level(2, "🍴", R.string.lvl_2,              200,   599),
        Level(3, "👨‍🍳", R.string.lvl_3,   600,  1499),
        Level(4, "🌟", R.string.lvl_4,               1500,  3999),
        Level(5, "🏆", R.string.lvl_5,               4000,  9999),
        Level(6, "💎", R.string.lvl_6,        10000, Int.MAX_VALUE)
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
    data class Badge(
        val id: String,
        val emoji: String,
        @StringRes val nameRes: Int,
        @StringRes val descriptionRes: Int,
    )

    val ALL_BADGES = listOf(
        Badge("first_plate",  "📸", R.string.bdg_first_plate, R.string.bdg_first_plate_d),
        Badge("globetrotter", "🌍", R.string.bdg_globetrotter, R.string.bdg_globetrotter_d),
        Badge("popular",      "❤️", R.string.bdg_popular, R.string.bdg_popular_d),
        Badge("critic",       "⭐", R.string.bdg_critic, R.string.bdg_critic_d),
        Badge("top10",        "🏆", R.string.bdg_top10, R.string.bdg_top10_d),
        // Lo concede closeWeeklyLeague al ganador de cada semana. Las versiones que no
        // conozcan este id simplemente no lo pintan (ProfileScreen descarta los nulos).
        Badge("league_winner", "🥇", R.string.bdg_league_winner, R.string.bdg_league_winner_d)
    )

    fun getBadge(id: String): Badge? = ALL_BADGES.find { it.id == id }
    // XP and badge writes are handled server-side by Cloud Functions (onRatingCreated, moderatePlateImage)
}
