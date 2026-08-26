package com.example.mealomat.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SupabaseAuthRepository(
    private val client: SupabaseClient,
    private val sessionData: SessionScopedData,
) : AuthRepository {

    override suspend fun currentUserId(): String? =
        client.auth.currentUserOrNull()?.id ?: client.auth.sessionManager.loadSession().user?.id

    override val state: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Initializing -> AuthState.Loading
            is SessionStatus.Authenticated -> AuthState.SignedIn
            // failed refresh is prolly almost always just being offline: stay signed in
            is SessionStatus.RefreshFailure -> AuthState.SignedIn
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
        }
    }

    // TODO: extend with additional provider mapping
    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.recoverCatching { throw AuthException(it.toUserMessage()) }

    override suspend fun signOut() {
        sessionData.clear()
        runCatching { client.auth.signOut() }
    }
}

// TODO: maybe map to dedicated AuthError enum and have message mapping in viewmodel
private fun Throwable.toUserMessage(): String = when {
    this is AuthRestException && errorCode == AuthErrorCode.InvalidCredentials ->
        "Email or password is incorrect."
    this is HttpRequestException -> "Can't reach the server. Signing in needs a connection."
    else -> "Something went wrong. Try again."
}
