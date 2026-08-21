package com.example.mealomat.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val userId: String?) : AuthState
}

class AuthException(val userMessage: String) : Exception(userMessage)
