package com.example.mealomat.core

import kotlin.coroutines.cancellation.CancellationException

// Result<T> cannot constrain its failure type; this can, so consumers get an exhaustive when.
sealed interface Outcome<out T> {
    data class Ok<out T>(val value: T) : Outcome<T>
    data class Fail(val error: AppError) : Outcome<Nothing>
}

fun <T> Result<T>.toOutcome(mapError: (Throwable) -> AppError): Outcome<T> = fold(
    onSuccess = { Outcome.Ok(it) },
    onFailure = { if (it is CancellationException) throw it else Outcome.Fail(mapError(it)) },
)
