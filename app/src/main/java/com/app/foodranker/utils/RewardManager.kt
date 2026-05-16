package com.app.foodranker.utils

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object RewardManager {

    // ── XP por acción ─────────────────────────────────────────────
    const val XP_PLATE_WITH_PHOTO = 50
    const val XP_PLATE_NO_PHOTO   = 10
    const val XP_GIVE_RATING      = 5
    const val XP_RECEIVE_RATING   = 10
    const val XP_RECEIVE_LIKE     = 3

    // ── Definición de niveles ──────────────────────────────────────
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
        Badge("streak_7",     "🔥", "Racha 7 días",      "7 días consecutivos activo"),
        Badge("top10",        "🏆", "Top 10",            "Un plato tuyo en el Top 10")
    )

    fun getBadge(id: String): Badge? = ALL_BADGES.find { it.id == id }

    // ── Otorgar XP ────────────────────────────────────────────────
    // Usamos una transacción para evitar pérdida de XP cuando dos valoraciones
    // llegan al mismo plato casi a la vez (read-then-write no atómico perdería
    // uno de los incrementos).
    suspend fun awardXP(userId: String, amount: Int, firestore: FirebaseFirestore) {
        if (userId.isEmpty() || userId.startsWith("seed")) return
        try {
            val ref = firestore.collection("users").document(userId)
            val newXP = firestore.runTransaction { tx ->
                val snap = tx.get(ref)
                val current = (snap.getLong("xp") ?: 0L).toInt()
                val updated = current + amount
                tx.update(ref, mapOf(
                    "xp" to updated,
                    "level" to getLevel(updated).number
                ))
                updated
            }.await()
            Log.d("RewardManager", "XP +$amount → usuario $userId (total: $newXP)")
        } catch (e: Exception) {
            Log.e("RewardManager", "Error otorgando XP: ${e.message}")
        }
    }

    // ── Comprobar y asignar badges ─────────────────────────────────
    suspend fun checkAndAwardBadges(userId: String, firestore: FirebaseFirestore) {
        if (userId.isEmpty() || userId.startsWith("seed")) return
        try {
            val userRef = firestore.collection("users").document(userId)
            val userDoc = userRef.get().await()
            @Suppress("UNCHECKED_CAST")
            val current = (userDoc.get("badges") as? List<String>) ?: emptyList()
            val earned = current.toMutableList()

            // Primer plato
            val platesSnap = firestore.collection("plates")
                .whereEqualTo("addedByUserId", userId).limit(1).get().await()
            if (platesSnap.size() >= 1 && "first_plate" !in earned) earned.add("first_plate")

            // Globetrotter (platos en 3+ países)
            val allPlates = firestore.collection("plates")
                .whereEqualTo("addedByUserId", userId).get().await()
            val countries = allPlates.documents.mapNotNull { it.getString("country") }.distinct()
            if (countries.size >= 3 && "globetrotter" !in earned) earned.add("globetrotter")

            // Crítico (10+ valoraciones dadas)
            val ratingsSnap = firestore.collection("ratings")
                .whereEqualTo("userId", userId).get().await()
            if (ratingsSnap.size() >= 10 && "critic" !in earned) earned.add("critic")

            // Popular (50+ likes recibidos sumando todos sus platos)
            val totalLikes = allPlates.documents.sumOf { (it.getLong("likes") ?: 0L).toInt() }
            if (totalLikes >= 50 && "popular" !in earned) earned.add("popular")

            if (earned != current) {
                userRef.update("badges", earned).await()
                Log.d("RewardManager", "Nuevos badges para $userId: ${earned - current}")
            }
        } catch (e: Exception) {
            Log.e("RewardManager", "Error comprobando badges: ${e.message}")
        }
    }
}
