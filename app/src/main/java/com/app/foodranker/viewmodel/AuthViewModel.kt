package com.app.foodranker.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.app.foodranker.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    val isLoggedIn: Boolean get() = authRepository.isLoggedIn
    val currentUser get() = authRepository.currentUser

    suspend fun awaitAuthReady(): Boolean = authRepository.awaitAuthReady()

    fun signInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            applyResult(authRepository.signInWithGoogle(activity))
        }
    }

    // Recupera un login que quedó a medias si la Activity se recreó mientras el
    // navegador estaba abierto (ver AuthRepository.awaitPendingGoogleSignIn).
    fun checkPendingGoogleSignIn() {
        if (!authRepository.hasPendingGoogleSignIn()) return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.awaitPendingGoogleSignIn() ?: return@launch
            applyResult(result)
        }
    }

    private fun applyResult(result: Result<FirebaseUser>) {
        if (result.isSuccess) {
            _authState.value = AuthState.Success(result.getOrThrow())
            return
        }
        val error = result.exceptionOrNull()
        // Cerrar la hoja de cuentas no es un fallo: se vuelve a Idle en silencio. Sin esto,
        // al usuario le saldría un "algo salió mal" por haber decidido no entrar.
        if (error is com.app.foodranker.data.repository.SignInCancelledException) {
            _authState.value = AuthState.Idle
            return
        }
        _authState.value = AuthState.Error(
            (error as? Exception)?.let { com.app.foodranker.utils.ErrorMapper.toUserMessage(it) }
                ?: "Error desconocido"
        )
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = AuthState.Idle
        }
    }
}