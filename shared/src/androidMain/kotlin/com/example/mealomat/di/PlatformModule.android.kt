package com.example.mealomat.di

import com.example.mealomat.data.db.DriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DriverFactory(get()) }
}

fun startKoinAndroid(context: android.content.Context, debug: Boolean) {
    startKoin {
        androidContext(context)
        modules(listOfNotNull(appModule, platformModule, devModule.takeIf { debug }))
    }
}
