package com.example.mealomat

import android.app.Application
import com.example.mealomat.di.startKoinAndroid

class MealomatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoinAndroid(this, debug = BuildConfig.DEBUG)
    }
}
