package com.app.foodranker.data.repository

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.app.foodranker.R
import com.app.foodranker.data.model.User
import com.app.foodranker.utils.InputLimits
import com.app.foodranker.utils.InputLimits.sanitized
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** El usuario cerró la hoja de cuentas. No es un error: no se le muestra nada. */
class SignInCancelledException : Exception("Login cancelado por el usuario")

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    private companion object { const val TAG = "AuthRepository" }

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

    // AVISO HISTÓRICO, importante antes de tocar nada de aquí: el login nativo YA falló
    // dos veces en builds reales de Play (primero GoogleSignInClient, luego Credential
    // Manager). Se atascaba en silencio porque el fallo ocurría dentro del propio proceso
    // de Play Services al validar la firma del paquete ("SignIn: Failed to record the
    // consent" en logcat) — ver docs/HANDOFF.md, sección "Décima sesión". Por eso la app
    // pasó a entrar solo por navegador, que autentica por client_id + redirect_uri y no
    // depende de que Play Services valide nada del paquete instalado.
    //
    // Hoy se vuelve a intentar el nativo, pero la diferencia es justo esa: si se repite
    // aquel fallo, ya no deja a nadie tirado — cae al navegador (ver signInWithGoogle).
    // Si alguna vez hay que quitar el nativo otra vez, basta con borrar tryNativeSignIn:
    // el camino de abajo es el que la app lleva usando en producción desde entonces.
    @Volatile private var signedOutSinceLastAttempt = false

    /**
     * Login con Google en dos intentos.
     *
     * 1. **Credential Manager** (nativo, hoja inferior de Android). No abre navegador, así que
     *    es inmune al fallo que deja fuera a quien tiene de predeterminado un navegador sin
     *    Chrome Custom Tabs — el de Xiaomi, por ejemplo, donde hoy el login da error.
     * 2. Si ese camino no puede completarse, **el flujo por navegador de siempre**, que se
     *    queda tal cual estaba.
     *
     * Los dos acaban en el mismo usuario de Firebase: ambos identifican a la persona por su
     * `sub` de Google en este proyecto, así que nadie pierde su cuenta ni su progreso por
     * entrar de una forma u otra.
     *
     * La cancelación **no** es un fallo: si alguien cierra la hoja de cuentas, no se le abre
     * un navegador a continuación — sería pelearse con él.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<FirebaseUser> {
        signedOutSinceLastAttempt = false
        tryNativeSignIn(activity)?.let { return it }
        return signInWithBrowser(activity)
    }

    /**
     * Devuelve el resultado si el camino nativo concluyó (con éxito o porque el usuario
     * canceló), o **null** si no se pudo usar y hay que tirar del navegador.
     */
    private suspend fun tryNativeSignIn(activity: Activity): Result<FirebaseUser>? {
        return try {
            val option = GetSignInWithGoogleOption
                .Builder(activity.getString(R.string.default_web_client_id))
                .build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = CredentialManager.create(activity)
                .getCredential(activity, request)
                .credential

            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                // Un tipo de credencial que no esperamos: mejor el navegador que fallar.
                Log.w(TAG, "Credencial inesperada (${credential.type}), se usará el navegador")
                return null
            }

            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            completeSignIn(auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)))
        } catch (e: GetCredentialCancellationException) {
            // Cerró la hoja de cuentas a propósito. Ni navegador ni mensaje de error.
            Result.failure(SignInCancelledException())
        } catch (e: Exception) {
            // Sin cuentas en el móvil, sin Play Services, error interno... da igual cuál:
            // cualquier otro fallo se resuelve intentándolo por el navegador.
            Log.w(TAG, "Login nativo no disponible (${e.javaClass.simpleName}), se usa el navegador: ${e.message}")
            null
        }
    }

    private suspend fun signInWithBrowser(activity: Activity): Result<FirebaseUser> {
        val provider = OAuthProvider.newBuilder("google.com")
            .addCustomParameter("prompt", "select_account")
            // Sin esto, un primer consentimiento (usuario nuevo que nunca usó FoodRanker)
            // le devuelve a Firebase la foto pero NO el nombre — visto en los 3 primeros
            // testers reales, mientras que las cuentas que ya llevaban meses seguían
            // mostrando bien el nombre porque Firebase conserva el que ya tenían guardado
            // de antes, no porque el flujo lo trajera de nuevo. Pedir el scope explícito
            // es la vía recomendada por Firebase para proveedores OAuth genéricos.
            .setScopes(listOf("email", "profile"))
            .build()
        return completeSignIn(auth.startActivityForSignInWithProvider(activity, provider))
    }

    // Si la Activity se recrea mientras el navegador está abierto (giro de pantalla, poca
    // memoria), el resultado del login no se pierde: Firebase lo guarda y hay que
    // recuperarlo con pendingAuthResult en vez de relanzar el flujo desde cero.
    // Chequeo síncrono para que el ViewModel pueda decidir si vale la pena mostrar el
    // spinner antes de lanzar la corrutina: se llama en cada apertura de la pantalla de
    // login, así que sin esto se vería un parpadeo de carga incluso sin nada pendiente.
    //
    // El guard de signedOutSinceLastAttempt es defensivo: no hay confirmación de que
    // FirebaseAuth limpie pendingAuthResult tras consumirlo una vez, así que sin esto un
    // cierre de sesión justo después de un login recuperado (tras recrear la Activity)
    // podría volver a autenticar solo con reabrir la pantalla de login, sin pulsar nada.
    fun hasPendingGoogleSignIn(): Boolean =
        !signedOutSinceLastAttempt && auth.pendingAuthResult != null

    suspend fun awaitPendingGoogleSignIn(): Result<FirebaseUser>? {
        val pending = auth.pendingAuthResult ?: return null
        return completeSignIn(pending)
    }

    private suspend fun completeSignIn(task: com.google.android.gms.tasks.Task<AuthResult>): Result<FirebaseUser> {
        return try {
            val result = task.await()
            val firebaseUser = result.user
                ?: return Result.failure(Exception("Error de autenticación: usuario nulo"))
            syncUserDocument(firebaseUser, result.additionalUserInfo?.profile)
            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncUserDocument(firebaseUser: FirebaseUser, rawProfile: Map<String, Any?>?) {
        // Para el flujo OAuth genérico, firebaseUser.displayName no siempre viene relleno
        // en el primer login de una cuenta nueva — el respaldo son los claims en bruto de
        // Google en additionalUserInfo.profile, que es donde Firebase documenta que hay
        // que mirar para proveedores OAuth genéricos (no pasa con el proveedor nativo de
        // Google, que sí rellena displayName siempre).
        val displayName = (
            firebaseUser.displayName?.takeIf { it.isNotBlank() }
                ?: (rawProfile?.get("name") as? String)?.takeIf { it.isNotBlank() }
                ?: "Usuario"
            ).sanitized(InputLimits.USER_NAME)
        val photoUrl = firebaseUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }
            ?: (rawProfile?.get("picture") as? String)
            ?: ""

        val userRef = firestore.collection("users").document(firebaseUser.uid)
        val snap = userRef.get().await()

        // El email NO se guarda en Firestore (el doc users/{uid} es legible por
        // cualquier usuario autenticado vía firestore.rules — guardar el email ahí
        // lo expondría a todo el mundo). Ya está disponible vía FirebaseAuth
        // (auth.currentUser?.email) para quien lo necesite localmente.
        if (!snap.exists()) {
            // Primer login: creamos el documento completo del usuario.
            // createdAt hay que pasarlo a mano: el default del modelo es 0L y, si no
            // se rellena aquí, todo usuario nuevo queda con fecha de alta en 1970.
            val newUser = User(
                id = firebaseUser.uid,
                name = displayName,
                photoUrl = photoUrl,
                createdAt = System.currentTimeMillis()
            )
            userRef.set(newUser).await()
        } else {
            // Logins posteriores: SOLO refrescamos los datos que vienen de Google,
            // sin tocar xp, level, badges, bio, isPremium, etc.
            userRef.update(
                mapOf(
                    "name" to displayName,
                    "photoUrl" to photoUrl
                )
            ).await()
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
        signedOutSinceLastAttempt = true
    }
}