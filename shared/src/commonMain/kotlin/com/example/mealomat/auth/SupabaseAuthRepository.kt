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

class SupabaseAuthRepository(private val client: SupabaseClient) : AuthRepository {

    override val state: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Initializing -> AuthState.Loading
            is SessionStatus.Authenticated -> AuthState.SignedIn(status.session.user?.id)
            // failed refresh is prolly almost always just being offline, so persisted session is read here (check if safe)
            is SessionStatus.RefreshFailure -> AuthState.SignedIn(client.auth.sessionManager.loadSession()?.user?.id)
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
        runCatching { client.auth.signOut() }
    }
}

private fun Throwable.toUserMessage(): String = when {
    this is AuthRestException && errorCode == AuthErrorCode.InvalidCredentials ->
        "Email or password is incorrect."
    this is HttpRequestException -> "Can't reach the server. Signing in needs a connection."
    else -> "Something went wrong. Try again."
}
