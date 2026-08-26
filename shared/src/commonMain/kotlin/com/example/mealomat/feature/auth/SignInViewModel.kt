package com.example.mealomat.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.core.AppError
import com.example.mealomat.core.Outcome
import com.example.mealomat.core.SignInError
import com.example.mealomat.feature.GENERIC_ERROR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SignInViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val outcome = auth.signIn(current.email, current.password)
            _state.update {
                it.copy(
                    isSubmitting = false,
                    error = (outcome as? Outcome.Fail)?.error?.toMessage(),
                )
            }
        }
    }
}

private fun AppError.toMessage(): String = when (this) {
    SignInError.InvalidCredentials -> "Email or password is incorrect."
    AppError.Offline -> "Can't reach the server. Signing in needs a connection."
    AppError.Unauthorized, is AppError.Server, AppError.Unknown -> GENERIC_ERROR
}
