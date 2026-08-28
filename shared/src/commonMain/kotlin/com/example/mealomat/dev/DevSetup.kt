package com.example.mealomat.dev

// Dev-only work that has to run inside the app, bound in debug builds by di/DevModule.kt.
fun interface DevSetup {
    suspend fun run()
}
