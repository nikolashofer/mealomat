package com.example.mealomat.auth

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data object SignedIn : AuthState
}
