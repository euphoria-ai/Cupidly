package com.tomfricks.cupidly

import android.app.Application
import com.tomfricks.cupidly.data.PreferencesRepository

class CupidlyApplication : Application() {
    
    lateinit var preferencesRepository: PreferencesRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
    }
}
