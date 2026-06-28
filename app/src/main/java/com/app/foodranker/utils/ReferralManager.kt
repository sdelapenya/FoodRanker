package com.app.foodranker.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferralManager @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun getOrCreateReferralCode(): String {
        val userId = auth.currentUser?.uid ?: return ""
        val userDoc = firestore.collection("users").document(userId).get().await()
        val existing = userDoc.getString("referralCode")
        if (!existing.isNullOrEmpty()) return existing

        val code = generateCode()
        firestore.collection("users").document(userId)
            .update("referralCode", code).await()
        return code
    }

    suspend fun applyReferralCode(code: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            // Buscar al referidor por código fuera de la transacción (solo lectura)
            val snapshot = firestore.collection("users")
                .whereEqualTo("referralCode", code)
                .limit(1).get().await()
            if (snapshot.isEmpty) return false
            val referrerId = snapshot.documents.first().id
            if (referrerId == userId) return false

            val myUserRef = firestore.collection("users").document(userId)
            val referrerRef = firestore.collection("users").document(referrerId)
            val referralRef = firestore.collection("referrals").document("${referrerId}_${userId}")

            // Transacción atómica: evita double-increment si hay llamadas concurrentes
            firestore.runTransaction { tx ->
                val myDoc = tx.get(myUserRef)
                if (!myDoc.getString("referredByUserId").isNullOrEmpty()) {
                    throw Exception("already_referred")
                }
                val referralDoc = tx.get(referralRef)
                if (referralDoc.exists()) throw Exception("already_referred")

                tx.set(referralRef, mapOf(
                    "referrerId" to referrerId,
                    "referredId" to userId,
                    "createdAt" to System.currentTimeMillis()
                ))
                // referralCount y XP los incrementa la Cloud Function onReferralCreated
                tx.update(myUserRef, mapOf("referredByUserId" to referrerId))
            }.await()

            // XP otorgado por onReferralCreated (Cloud Function via Admin SDK)
            true
        } catch (e: Exception) {
            if (e.message == "already_referred") false
            else throw e
        }
    }

    fun buildReferralShareText(code: String, userName: String): String =
        "👋 ¡$userName te invita a FoodRanker!\n\n" +
        "🍽️ Descubre y valora los mejores platos del mundo.\n" +
        "Usa mi código de invitación: $code\n\n" +
        "Descárgala aquí 👇\n" +
        "https://foodranker.app/invite/$code\n\n" +
        "#FoodRanker #Gastronomia"

    private fun generateCode(): String = UUID.randomUUID().toString()
        .replace("-", "").take(10).uppercase()
}
