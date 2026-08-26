package com.example.mealomat.testing

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAuth(private val userId: String?) : AuthRepository {
    override val state: Flow<AuthState> =
        flowOf(if (userId == null) AuthState.SignedOut else AuthState.SignedIn)

    override suspend fun currentUserId(): String? = userId
    override suspend fun signIn(email: String, password: String) = Result.success(Unit)
    override suspend fun signOut() = Unit
}
