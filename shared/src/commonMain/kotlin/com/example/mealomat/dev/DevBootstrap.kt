package com.example.mealomat.dev

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mealomat.ui.components.SplashScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin

// Runs the registered DevSetup, then renders `content` unchanged; a no-op when none is bound.
@Composable
fun DevBootstrap(content: @Composable () -> Unit) {
    val setup = getKoin().getOrNull<DevSetup>() ?: return content()

    var done by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) { setup.run() }
        done = true
    }

    if (done) content() else SplashScreen()
}
