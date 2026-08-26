package com.example.mealomat.auth

import com.example.mealomat.core.Outcome
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val state: Flow<AuthState>
    suspend fun currentUserId(): String?
    suspend fun signIn(email: String, password: String): Outcome<Unit>
    suspend fun signOut(): Outcome<Unit>
}

// no user is a bug in caller, not a failure to render: throws deliberately
suspend fun AuthRepository.requireUserId(): String =
    requireNotNull(currentUserId()) { "No signed-in user" }
