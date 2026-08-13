package com.tomfricks.hook

import android.app.Application
import android.util.Log
import com.tomfricks.hook.api.ApiService
import com.tomfricks.hook.billing.BillingManager
import com.tomfricks.hook.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HookApplication : Application() {

    lateinit var preferencesRepository: PreferencesRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)

        // Runs for every process entry point — the launcher activity, the IME
        // and the screenshot service all share this Application instance.
        appScope.launch {
            // Mint the install id first: both the billing SDK and every API
            // request are keyed by it.
            val appUserId = preferencesRepository.getOrCreateAppUserId()
            BillingManager.configure(this@HookApplication, appUserId, preferencesRepository)

            // Refresh the cached allowance so the keyboard's "N free left" is
            // right before the user generates anything this session.
            ApiService(this@HookApplication).fetchEntitlement()
                .onFailure { Log.d("HookApplication", "Entitlement refresh skipped: ${it.message}") }
        }
    }
}
