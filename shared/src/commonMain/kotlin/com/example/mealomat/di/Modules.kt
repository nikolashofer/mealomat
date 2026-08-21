package com.example.mealomat.di

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.SupabaseAuthRepository
import com.example.mealomat.auth.createSupabase
import com.example.mealomat.feature.auth.SignInViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { createSupabase() }
    single<AuthRepository> { SupabaseAuthRepository(get()) }
    viewModel { SignInViewModel(get()) }
}

fun initKoin() {
    startKoin { modules(appModule) }
}
