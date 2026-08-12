package com.tomfricks.cupidly.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import com.tomfricks.cupidly.BuildConfig
import com.tomfricks.cupidly.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What came back from a purchase or a restore, in terms the UI cares about. */
sealed interface PurchaseResult {
    data object Success : PurchaseResult

    /** The user backed out of the Play sheet — not an error, say nothing. */
    data object Cancelled : PurchaseResult

    data class Failed(val message: String) : PurchaseResult
}

/**
 * Thin wrapper around the RevenueCat SDK.
 *
 * A process-wide singleton because the paywall (an Activity), the keyboard IME
 * and the screenshot service all need the same entitlement answer, and
 * RevenueCat's own `Purchases` is a singleton anyway.
 *
 * Everything degrades quietly when [BuildConfig.REVENUECAT_PUBLIC_SDK_KEY] is
 * blank, so a debug build with no keys in `local.properties` still runs: the
 * SDK is simply never configured and [isAvailable] stays false.
 */
object BillingManager {

    /** Entitlement identifier configured in the RevenueCat dashboard. */
    const val ENTITLEMENT_PRO = "pro"

    private const val TAG = "BillingManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var preferencesRepository: PreferencesRepository? = null

    private val _isPro = MutableStateFlow(false)

    /** True while the "pro" entitlement is active. Painted from cache, then confirmed. */
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _currentOffering = MutableStateFlow<Offering?>(null)

    /** The current RevenueCat offering, or null until [refreshOfferings] answers. */
    val currentOffering: StateFlow<Offering?> = _currentOffering.asStateFlow()

    private val _isLoadingOfferings = MutableStateFlow(false)
    val isLoadingOfferings: StateFlow<Boolean> = _isLoadingOfferings.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)

    /** False when there is no SDK key, i.e. no purchase can be made at all. */
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    /**
     * Configure the SDK against this install's stable id, so entitlements
     * follow the same identity the server meters the free allowance by.
     *
     * Safe to call more than once; the second call is a no-op.
     */
    fun configure(context: Context, appUserId: String, repository: PreferencesRepository) {
        preferencesRepository = repository

        // Paint from the persisted answer first so Pro users never flash a
        // paywall while the network catches up.
        scope.launch {
            val cached = runCatching { repository.entitlementFlow.first().isPro }.getOrNull()
            if (cached == true) {
                _isPro.value = true
            }
        }

        val apiKey = BuildConfig.REVENUECAT_PUBLIC_SDK_KEY
        if (apiKey.isBlank()) {
            Log.w(
                TAG,
                "No REVENUECAT_PUBLIC_SDK_KEY in local.properties — purchases are disabled"
            )
            return
        }

        if (Purchases.isConfigured) {
            _isAvailable.value = true
            return
        }

        Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        Purchases.configure(
            PurchasesConfiguration.Builder(context.applicationContext, apiKey)
                .appUserID(appUserId)
                .build()
        )
        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { customerInfo -> onCustomerInfo(customerInfo) }

        _isAvailable.value = true
        refreshCustomerInfo()
        refreshOfferings()
    }

    /** Re-read the entitlement from RevenueCat (launch, resume, after a purchase). */
    fun refreshCustomerInfo() {
        if (!Purchases.isConfigured) return
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error -> Log.e(TAG, "Could not load customer info: ${error.message}") },
            onSuccess = { customerInfo -> onCustomerInfo(customerInfo) }
        )
    }

    /** Load the current offering and its packages for the paywall. */
    fun refreshOfferings() {
        if (!Purchases.isConfigured) return
        _isLoadingOfferings.value = true
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error ->
                _isLoadingOfferings.value = false
                Log.e(TAG, "Could not load offerings: ${error.message}")
            },
            onSuccess = { offerings ->
                _isLoadingOfferings.value = false
                _currentOffering.value = offerings.current
            }
        )
    }

    /**
     * Start the Play purchase flow. RevenueCat needs a real [Activity] to host
     * the billing sheet, which is why the keyboard hands the user off to
     * MainActivity instead of buying in-place.
     */
    fun purchase(activity: Activity, packageToPurchase: Package, onResult: (PurchaseResult) -> Unit) {
        if (!Purchases.isConfigured) {
            onResult(PurchaseResult.Failed("Purchases aren't available right now"))
            return
        }
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, packageToPurchase).build(),
            onError = { error, userCancelled ->
                if (userCancelled) {
                    onResult(PurchaseResult.Cancelled)
                } else {
                    Log.e(TAG, "Purchase failed: ${error.message}")
                    onResult(PurchaseResult.Failed(error.message))
                }
            },
            onSuccess = { _, customerInfo ->
                onCustomerInfo(customerInfo)
                onResult(
                    if (customerInfo.hasPro()) {
                        PurchaseResult.Success
                    } else {
                        PurchaseResult.Failed("Purchase went through but Pro isn't active yet")
                    }
                )
            }
        )
    }

    /** Bring back a subscription bought on another install of the same account. */
    fun restorePurchases(onResult: (PurchaseResult) -> Unit) {
        if (!Purchases.isConfigured) {
            onResult(PurchaseResult.Failed("Purchases aren't available right now"))
            return
        }
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                Log.e(TAG, "Restore failed: ${error.message}")
                onResult(PurchaseResult.Failed(error.message))
            },
            onSuccess = { customerInfo ->
                onCustomerInfo(customerInfo)
                onResult(
                    if (customerInfo.hasPro()) {
                        PurchaseResult.Success
                    } else {
                        PurchaseResult.Failed("No active Hook Pro subscription found")
                    }
                )
            }
        )
    }

    private fun CustomerInfo.hasPro(): Boolean =
        entitlements[ENTITLEMENT_PRO]?.isActive == true

    /** Single funnel for every entitlement update: state flow + persisted cache. */
    private fun onCustomerInfo(customerInfo: CustomerInfo) {
        val pro = customerInfo.hasPro()
        _isPro.value = pro

        val repository = preferencesRepository ?: return
        scope.launch {
            runCatching { repository.updateIsPro(pro) }
                .onFailure { Log.w(TAG, "Could not cache entitlement", it) }
        }
    }
}
