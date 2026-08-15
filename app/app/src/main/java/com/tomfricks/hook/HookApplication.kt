package com.tomfricks.hook

import android.app.Application
import android.util.Log
import com.tomfricks.hook.api.ApiService
import com.tomfricks.hook.billing.BillingManager
import com.tomfricks.hook.data.OnboardingSync
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

            // Wait for Play to attach any existing subscription to this install
            // before asking the server. Otherwise /me caches "not Pro" for a
            // subscriber who just reinstalled, and generations 402 until TTL.
            BillingManager.awaitPurchasesSynced()

            // Refresh the cached allowance so the keyboard's "N free left" is
            // right before the user generates anything this session.
            ApiService(this@HookApplication).fetchEntitlement()
                .onFailure { Log.d("HookApplication", "Entitlement refresh skipped: ${it.message}") }

            // Catches the profile of anyone who finished onboarding while
            // offline. A no-op for everyone else, including users who never
            // finished and users whose profile already went out.
            OnboardingSync.syncIfNeeded(this@HookApplication, preferencesRepository)
        }
    }
}
