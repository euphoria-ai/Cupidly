package com.tomlin7.cupidly

import android.app.Application
import com.tomlin7.cupidly.data.PreferencesRepository

class CupidlyApplication : Application() {
    
    lateinit var preferencesRepository: PreferencesRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
    }
}
