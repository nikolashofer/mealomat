package com.example.mealomat.auth

import com.example.mealomat.core.AppError
import com.example.mealomat.core.Outcome
import com.example.mealomat.core.SignInError
import com.example.mealomat.core.toOutcome
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
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
    override suspend fun signIn(email: String, password: String): Outcome<Unit> =
        runCatching {
            client.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
        }.toOutcome(Throwable::toAppError)

    override suspend fun signOut(): Outcome<Unit> {
        sessionData.clear()
        return runCatching { client.auth.signOut() }.toOutcome(Throwable::toAppError)
    }
}

private fun Throwable.toAppError(): AppError = when (this) {
    is AuthRestException if errorCode == AuthErrorCode.InvalidCredentials ->
        SignInError.InvalidCredentials
    is HttpRequestException -> AppError.Offline
    is RestException if statusCode == 401 -> AppError.Unauthorized
    is RestException -> AppError.Server(statusCode)
    else -> AppError.Unknown
}
