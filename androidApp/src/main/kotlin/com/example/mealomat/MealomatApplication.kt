package com.example.mealomat

import android.app.Application
import com.example.mealomat.di.initKoin

class MealomatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin()
    }
}
