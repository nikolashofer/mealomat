package com.example.mealomat.testing

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.AuthState
import com.example.mealomat.core.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// fake for Supabase in tests. `userId = null` is the signed-out case.
class FakeAuth(
    private val userId: String?,
    private val signInResult: Outcome<Unit> = Outcome.Ok(Unit),
    private val signOutResult: Outcome<Unit> = Outcome.Ok(Unit),
) : AuthRepository {
    override val state: Flow<AuthState> =
        flowOf(if (userId == null) AuthState.SignedOut else AuthState.SignedIn)

    override suspend fun currentUserId(): String? = userId
    override suspend fun signIn(email: String, password: String): Outcome<Unit> = signInResult
    override suspend fun signOut(): Outcome<Unit> = signOutResult
}
