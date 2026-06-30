package com.app.foodranker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.app.foodranker.data.model.Plate
import com.app.foodranker.data.model.PlateCategory
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlateRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val platesCollection = firestore.collection("plates")

    suspend fun getTopPlates(limit: Long = 10): Result<List<Plate>> {
        return try {
            val snapshot = platesCollection
                .orderBy("averageScore", Query.Direction.DESCENDING)
                .limit(limit + 5)  // over-fetch para compensar los filtrados por reportCount
                .get()
                .await()
            val plates = snapshot.documents
                .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                .filter { it.reportCount < 3 }
                .take(limit.toInt())
            Result.success(plates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlatesByCategory(category: PlateCategory): Result<List<Plate>> {
        return try {
            val limit = 20L
            val snapshot = platesCollection
                .whereEqualTo("category", category.name)
                .orderBy("averageScore", Query.Direction.DESCENDING)
                .limit(limit + 5) // over-fetch para compensar los filtrados por reportCount
                .get()
                .await()
            val plates = snapshot.documents
                .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                .filter { it.reportCount < 3 }
                .take(limit.toInt())
            Result.success(plates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentPlates(limit: Long = 10): Result<List<Plate>> {
        return try {
            val snapshot = platesCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()
            val plates = snapshot.documents
                .mapNotNull { it.toObject(Plate::class.java)?.copy(id = it.id) }
                .filter { it.reportCount < 3 }
            Result.success(plates)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Devuelve true si ahora está liked, false si ahora está unliked.
    // Usamos una transacción para que doble-tap o cliques concurrentes no
    // dejen `likes` y `likedByUsers` desincronizados.
    suspend fun toggleLike(plateId: String, userId: String): Result<Boolean> {
        return try {
            val plateRef = platesCollection.document(plateId)
            val nowLiked = firestore.runTransaction { tx ->
                val snap = tx.get(plateRef)
                @Suppress("UNCHECKED_CAST")
                val likedByUsers = (snap.get("likedByUsers") as? List<String>) ?: emptyList()
                val currentLikes = (snap.getLong("likes") ?: 0L).toInt()
                val isCurrentlyLiked = userId in likedByUsers

                if (isCurrentlyLiked) {
                    tx.update(plateRef, mapOf(
                        "likedByUsers" to (likedByUsers - userId),
                        "likes" to (currentLikes - 1).coerceAtLeast(0)
                    ))
                    false
                } else {
                    tx.update(plateRef, mapOf(
                        "likedByUsers" to (likedByUsers + userId),
                        "likes" to (currentLikes + 1)
                    ))
                    true
                }
            }.await()
            Result.success(nowLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
