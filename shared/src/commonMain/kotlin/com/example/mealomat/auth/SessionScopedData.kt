package com.example.mealomat.auth

// data belonging to the signed-in session, cleared when it ends
fun interface SessionScopedData {
    suspend fun clear()
}
