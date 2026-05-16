package com.app.foodranker.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.data.model.User
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user!!

            val displayName = (firebaseUser.displayName ?: "Usuario").sanitized(InputLimits.USER_NAME)
            val email = firebaseUser.email ?: ""
            val photoUrl = firebaseUser.photoUrl?.toString() ?: ""

            val userRef = firestore.collection("users").document(firebaseUser.uid)
            val snap = userRef.get().await()

            if (!snap.exists()) {
                // Primer login: creamos el documento completo del usuario
                val newUser = User(
                    id = firebaseUser.uid,
                    name = displayName,
                    email = email,
                    photoUrl = photoUrl
                )
                userRef.set(newUser).await()
            } else {
                // Logins posteriores: SOLO refrescamos los datos que vienen de Google,
                // sin tocar xp, level, badges, bio, isPremium, etc.
                userRef.update(
                    mapOf(
                        "name" to displayName,
                        "email" to email,
                        "photoUrl" to photoUrl
                    )
                ).await()
            }

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() = auth.signOut()
}