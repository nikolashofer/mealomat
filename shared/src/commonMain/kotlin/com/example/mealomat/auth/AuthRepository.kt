package com.example.mealomat.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val state: Flow<AuthState>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
}
