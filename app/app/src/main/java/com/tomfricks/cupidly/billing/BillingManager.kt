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
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
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

    /** The user backed out of the purchase sheet — not an error, say nothing. */
    data object Cancelled : PurchaseResult

    data class Failed(val message: String) : PurchaseResult
}

/**
 * Where the current offering stands. The paywall needs to tell "still loading"
 * apart from "the dashboard has no offering", because only the second case
 * should fall back to the hand-rolled screen.
 */
sealed interface OfferingsState {
    data object Idle : OfferingsState

    data object Loading : OfferingsState

    /** A fetch completed. [offering] is null when no current offering is configured. */
    data class Ready(val offering: Offering?) : OfferingsState

    data class Failed(val message: String) : OfferingsState
}

/**
 * Thin wrapper around the RevenueCat SDK (v9).
 *
 * A process-wide singleton because the paywall (an Activity), the keyboard IME
 * and the screenshot service all need the same entitlement answer, and
 * RevenueCat's own `Purchases` is a singleton anyway.
 *
 * Everything degrades quietly when [BuildConfig.REVENUECAT_PUBLIC_SDK_KEY] is
 * blank, so a debug build with no keys in `local.properties` still runs: the
 * SDK is simply never configured and [isAvailable] stays false.
 *
 * The key's prefix (`goog_` for Google Play, `test_` for RevenueCat's Test
 * Store) selects the backing store inside the SDK. Nothing here assumes one or
 * the other.
 */
object BillingManager {

    /**
     * The one place the entitlement identifier is written down.
     *
     * Must match the identifier in the RevenueCat dashboard and the server's
     * `REVENUECAT_ENTITLEMENT_ID`. "Hook Pro" is only the display name.
     */
    const val ENTITLEMENT_ID = "Hook Pro"

    /** Human-readable name for the thing [ENTITLEMENT_ID] unlocks. */
    const val PRO_DISPLAY_NAME = "Hook Pro"

    private const val TAG = "BillingManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var preferencesRepository: PreferencesRepository? = null

    private val _isPro = MutableStateFlow(false)

    /** True while the [ENTITLEMENT_ID] entitlement is active. Cache first, then confirmed. */
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _offeringsState = MutableStateFlow<OfferingsState>(OfferingsState.Idle)

    /** The current offering and how the last fetch went. */
    val offeringsState: StateFlow<OfferingsState> = _offeringsState.asStateFlow()

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

        // Entitlement only — deliberately *not* offerings. This runs from
        // Application.onCreate, and RevenueCat pre-fetches the offerings cache
        // on its own; asking here just adds a network request at launch for a
        // paywall the user may never open. PaywallScreen fetches on entry.
        scope.launch { refreshCustomerInfo() }
    }

    /** Fire-and-forget refresh for callers that aren't in a coroutine (Activity.onResume). */
    fun refreshInBackground() {
        scope.launch {
            refreshCustomerInfo()
            refreshOfferings()
        }
    }

    /** Re-read the entitlement from RevenueCat (launch, resume, after a purchase). */
    suspend fun refreshCustomerInfo() {
        if (!Purchases.isConfigured) return
        try {
            onCustomerInfo(Purchases.sharedInstance.awaitCustomerInfo())
        } catch (e: PurchasesException) {
            Log.e(TAG, "Could not load customer info: ${e.message}")
        }
    }

    /**
     * Load the current offering. Whatever the dashboard says is current wins —
     * the app never assumes a particular set of packages.
     */
    suspend fun refreshOfferings() {
        if (!Purchases.isConfigured) return
        _offeringsState.value = OfferingsState.Loading
        _offeringsState.value = try {
            OfferingsState.Ready(Purchases.sharedInstance.awaitOfferings().current)
        } catch (e: PurchasesException) {
            Log.e(TAG, "Could not load offerings: ${e.message}")
            OfferingsState.Failed(e.message)
        }
    }

    /**
     * Start the purchase flow. RevenueCat needs a real [Activity] to host the
     * billing sheet, which is why the keyboard hands the user off to
     * MainActivity instead of buying in-place.
     */
    suspend fun purchase(activity: Activity, packageToPurchase: Package): PurchaseResult {
        if (!Purchases.isConfigured) {
            return PurchaseResult.Failed("Purchases aren't available right now")
        }
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(
                PurchaseParams.Builder(activity, packageToPurchase).build()
            )
            onCustomerInfo(result.customerInfo)
            if (result.customerInfo.hasPro()) {
                PurchaseResult.Success
            } else {
                PurchaseResult.Failed("Purchase went through but $PRO_DISPLAY_NAME isn't active yet")
            }
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) {
                PurchaseResult.Cancelled
            } else {
                Log.e(TAG, "Purchase failed: ${e.message}")
                PurchaseResult.Failed(e.message)
            }
        } catch (e: PurchasesException) {
            Log.e(TAG, "Purchase failed: ${e.message}")
            PurchaseResult.Failed(e.message)
        }
    }

    /** Bring back a subscription bought on another install of the same account. */
    suspend fun restorePurchases(): PurchaseResult {
        if (!Purchases.isConfigured) {
            return PurchaseResult.Failed("Purchases aren't available right now")
        }
        return try {
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            onCustomerInfo(customerInfo)
            if (customerInfo.hasPro()) {
                PurchaseResult.Success
            } else {
                PurchaseResult.Failed("No active $PRO_DISPLAY_NAME subscription found")
            }
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) {
                PurchaseResult.Cancelled
            } else {
                Log.e(TAG, "Restore failed: ${e.message}")
                PurchaseResult.Failed(e.message)
            }
        } catch (e: PurchasesException) {
            Log.e(TAG, "Restore failed: ${e.message}")
            PurchaseResult.Failed(e.message)
        }
    }

    // Prefer the configured identifier, but for this single-entitlement app also
    // treat *any* active entitlement as Pro. This defends against a RevenueCat
    // dashboard identifier that doesn't match ENTITLEMENT_ID (e.g. "pro" instead
    // of "Hook Pro"), which otherwise leaves a paying user stuck on the paywall.
    private fun CustomerInfo.hasPro(): Boolean =
        entitlements[ENTITLEMENT_ID]?.isActive == true || entitlements.active.isNotEmpty()

    /**
     * Single funnel for every entitlement update: state flow + persisted cache.
     *
     * Public because RevenueCat's own Paywall and Customer Center hand us a
     * fresh [CustomerInfo] through their listeners, and that has to land in the
     * same place as our own refreshes.
     */
    fun onCustomerInfo(customerInfo: CustomerInfo) {
        val pro = customerInfo.hasPro()
        _isPro.value = pro

        val repository = preferencesRepository ?: return
        scope.launch {
            runCatching { repository.updateIsPro(pro) }
                .onFailure { Log.w(TAG, "Could not cache entitlement", it) }
        }
    }
}
