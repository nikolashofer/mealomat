package com.example.mealomat.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mealomat.auth.AuthException
import com.example.mealomat.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val GENERIC_ERROR = "Something went wrong. Try again."

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
            val result = auth.signIn(current.email, current.password)
            val failure = result.exceptionOrNull()
            _state.update {
                it.copy(
                    isSubmitting = false,
                    error = failure?.let {
                        (it as? AuthException)?.userMessage ?: GENERIC_ERROR
                    },
                )
            }
        }
    }
}
