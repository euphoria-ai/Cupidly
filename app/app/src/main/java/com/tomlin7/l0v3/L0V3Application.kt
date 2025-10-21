package com.tomlin7.l0v3

import android.app.Application
import com.tomlin7.l0v3.data.PreferencesRepository

class L0V3Application : Application() {
    
    lateinit var preferencesRepository: PreferencesRepository
        private set
    
    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)
    }
}
