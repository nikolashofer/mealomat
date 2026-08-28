package com.example.mealomat.di

import com.example.mealomat.data.seed.SeedImporter
import com.example.mealomat.data.seed.SeedOnFirstRun
import com.example.mealomat.dev.DevSetup
import org.koin.dsl.module

// Debug-only Koin bindings: everything dev-only that runs inside the app is registered here.
val devModule = module {
    single { SeedImporter(get(), get(), get()) }
    single<DevSetup> { SeedOnFirstRun(get(), get(), get(), get()) }
}
