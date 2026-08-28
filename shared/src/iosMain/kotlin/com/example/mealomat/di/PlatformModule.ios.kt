package com.example.mealomat.di

import com.example.mealomat.data.db.DriverFactory
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DriverFactory() }
}

fun startKoinIos(debug: Boolean) {
    startKoin { modules(listOfNotNull(appModule, platformModule, devModule.takeIf { debug })) }
}
