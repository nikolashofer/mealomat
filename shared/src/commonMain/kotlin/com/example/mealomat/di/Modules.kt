package com.example.mealomat.di

import com.example.mealomat.auth.AuthRepository
import com.example.mealomat.auth.SessionScopedData
import com.example.mealomat.auth.SupabaseAuthRepository
import com.example.mealomat.auth.createSupabase
import kotlin.time.Clock
import com.example.mealomat.data.db.DriverFactory
import com.example.mealomat.data.db.DatabaseSessionScopedData
import com.example.mealomat.data.db.mealomatDatabase
import com.example.mealomat.data.repo.IngredientRepository
import com.example.mealomat.data.repo.PlanRepository
import com.example.mealomat.data.repo.DayRepository
import com.example.mealomat.data.repo.PantryRepository
import com.example.mealomat.data.repo.PrepBlockRepository
import com.example.mealomat.feature.auth.SignInViewModel
import com.example.mealomat.feature.home.HomeViewModel
import com.example.mealomat.data.sync.OutboxWriter
import app.cash.sqldelight.db.SqlDriver
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// TODO: split into separate modules, either co-located or all in here: https://insert-koin.io/docs/reference/koin-core/modules/
val appModule = module {
    single { createSupabase() }
    single<SessionScopedData> { DatabaseSessionScopedData(get()) }
    single<AuthRepository> { SupabaseAuthRepository(get(), get()) }
    single<Clock> { Clock.System }
    single<SqlDriver> { get<DriverFactory>().create() }
    single { mealomatDatabase(get()) }
    single { OutboxWriter(get(), get()) }
    single { IngredientRepository(get(), get(), get(), get()) }
    single { PlanRepository(get(), get(), get(), get()) }
    single { PrepBlockRepository(get(), get(), get(), get()) }
    single { PantryRepository(get(), get(), get(), get()) }
    single { DayRepository(get(), get(), get(), get(), get(), get()) }
    viewModel { SignInViewModel(get()) }
    viewModel { HomeViewModel(get(), get()) }
}
