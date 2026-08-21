package com.example.mealomat

import androidx.compose.ui.window.ComposeUIViewController
import com.example.mealomat.di.initKoin

fun startDependencyInjection() = initKoin()

fun MainViewController() = ComposeUIViewController { App() }
