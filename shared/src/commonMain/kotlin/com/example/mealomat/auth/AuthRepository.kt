package com.example.mealomat.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val state: Flow<AuthState>
    suspend fun currentUserId(): String?
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
}

// TODO: properly handle null case
suspend fun AuthRepository.requireUserId(): String =
    requireNotNull(currentUserId()) { "No signed-in user" }
