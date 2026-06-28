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

    // Espera a que FirebaseAuth restaure la sesión persistida desde disco antes
    // de devolver el estado de login. Sin esto, currentUser puede ser null
    // momentáneamente al arrancar la app, enviando a un usuario logueado a Auth.
    suspend fun awaitAuthReady(): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val listener = object : FirebaseAuth.AuthStateListener {
            override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                auth.removeAuthStateListener(this)
                if (cont.isActive) cont.resume(firebaseAuth.currentUser != null) {}
            }
        }
        auth.addAuthStateListener(listener)
        cont.invokeOnCancellation { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Error de autenticación: usuario nulo"))

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

    suspend fun signOut() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                firestore.collection("users").document(userId)
                    .update("fcmToken", "").await()
            } catch (e: Exception) { /* sin red u otro fallo: no bloquear el logout */ }
        }
        auth.signOut()
    }
}