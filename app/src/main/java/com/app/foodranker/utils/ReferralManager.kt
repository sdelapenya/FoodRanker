package com.app.foodranker.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

object ReferralManager {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Genera o recupera el código de referido del usuario
    suspend fun getOrCreateReferralCode(): String {
        val userId = auth.currentUser?.uid ?: return ""
        val userDoc = firestore.collection("users").document(userId).get().await()
        val existing = userDoc.getString("referralCode")
        if (!existing.isNullOrEmpty()) return existing

        // Crear código nuevo
        val code = generateCode()
        firestore.collection("users").document(userId)
            .update("referralCode", code).await()
        return code
    }

    // Registrar uso de código de referido
    suspend fun applyReferralCode(code: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            val myUserRef = firestore.collection("users").document(userId)
            val myDoc = myUserRef.get().await()
            if (!myDoc.getString("referredByUserId").isNullOrEmpty()) return false

            val alreadyReferred = firestore.collection("referrals")
                .whereEqualTo("referredId", userId)
                .limit(1)
                .get()
                .await()
            if (!alreadyReferred.isEmpty) return false

            val snapshot = firestore.collection("users")
                .whereEqualTo("referralCode", code)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) return false
            val referrerId = snapshot.documents.first().id
            if (referrerId == userId) return false

            val referralRef = firestore.collection("referrals").document()
            val batch = firestore.batch()
            batch.set(
                referralRef,
                mapOf(
                    "referrerId" to referrerId,
                    "referredId" to userId,
                    "createdAt" to System.currentTimeMillis()
                )
            )
            batch.update(
                firestore.collection("users").document(referrerId),
                "referralCount",
                FieldValue.increment(1)
            )
            batch.update(myUserRef, mapOf("referredByUserId" to referrerId))
            batch.commit().await()

            true
        } catch (e: Exception) {
            false
        }
    }

    fun buildReferralShareText(code: String, userName: String): String {
        return "👋 ¡$userName te invita a FoodRanker!\n\n" +
                "🍽️ Descubre y valora los mejores platos del mundo.\n" +
                "Usa mi código de invitación: $code\n\n" +
                "Descárgala aquí 👇\n" +
                "https://foodranker.app/invite/$code\n\n" +
                "#FoodRanker #Gastronomia"
    }

    private fun generateCode(): String {
        return UUID.randomUUID().toString().take(8).uppercase()
    }
}