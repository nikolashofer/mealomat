package com.example.mealomat.core

sealed interface AppError {
    data object Offline : AppError
    data object Unauthorized : AppError
    data class Server(val code: Int?) : AppError
    data object Unknown : AppError
}

sealed interface SignInError : AppError {
    data object InvalidCredentials : SignInError
}
