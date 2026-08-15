package com.tomfricks.hook.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.tomfricks.hook.BuildConfig
import com.tomfricks.hook.api.ApiService
import com.tomfricks.hook.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** What came back from a purchase or a restore, in terms the UI cares about. */
sealed interface PurchaseResult {
    data object Success : PurchaseResult

    /** The user backed out of the purchase sheet — not an error, say nothing. */
    data object Cancelled : PurchaseResult

    data class Failed(val message: String) : PurchaseResult
}

/** Which flow produced a [PurchaseResult] — the two need different success copy. */
enum class BillingOperation { PURCHASE, RESTORE }

/**
 * A finished purchase or restore, published by [BillingManager] rather than
 * returned to the caller.
 *
 * The distinction matters: the coroutine that runs the purchase belongs to the
 * singleton, so the outcome survives an Activity recreation that happens while
 * the Play billing sheet is up. Whichever paywall is alive afterwards reads it.
 */
data class BillingOutcome(val operation: BillingOperation, val result: PurchaseResult)

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
 * Thin wrapper around the RevenueCat SDK (v10).
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

    /**
     * How hard to chase an entitlement that a completed purchase hasn't shown
     * us yet. Three tries 800ms apart is a ~2.4s worst case — long enough for
     * the backend to catch up, short enough that the paywall button doesn't
     * feel stuck.
     */
    private const val ENTITLEMENT_RECHECK_ATTEMPTS = 3
    private const val ENTITLEMENT_RECHECK_DELAY_MS = 800L

    /**
     * Play Billing can stall. After this we stop waiting and let the UI
     * proceed on whatever CustomerInfo we already have, rather than freezing
     * onboarding or the paywall on a dead store connection.
     */
    private const val STORE_SYNC_TIMEOUT_MS = 15_000L

    /** Slightly longer than the sync itself, so waiters see [purchasesSynced]. */
    private const val STORE_SYNC_WAIT_MS = STORE_SYNC_TIMEOUT_MS + 2_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** One in-flight store sync at a time, so a Restore tap can't race launch. */
    private val storeSyncMutex = Mutex()

    private var preferencesRepository: PreferencesRepository? = null

    /**
     * Application context only — never an Activity. Used to reach the server's
     * entitlement refresh with the app's normal auth headers after a purchase.
     */
    private var appContext: Context? = null

    private val _isPro = MutableStateFlow(false)

    /** True while the [ENTITLEMENT_ID] entitlement is active. Cache first, then confirmed. */
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _offeringsState = MutableStateFlow<OfferingsState>(OfferingsState.Idle)

    /** The current offering and how the last fetch went. */
    val offeringsState: StateFlow<OfferingsState> = _offeringsState.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)

    /** False when there is no SDK key, i.e. no purchase can be made at all. */
    val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private val _purchasesSynced = MutableStateFlow(false)

    /**
     * True once this process has asked Play for existing purchases (or decided
     * it never will, e.g. no SDK key). The paywall must not open before this:
     * a subscriber who reinstalled has a new install id, and until Play has
     * attached their subscription to it they look like a free user.
     */
    val purchasesSynced: StateFlow<Boolean> = _purchasesSynced.asStateFlow()

    private val _purchaseInFlight = MutableStateFlow(false)

    /**
     * True from the moment a purchase or restore is started until its outcome is
     * published. Lives here rather than in the paywall's composition so the
     * button still reads "Working…" — and stays guarded — after an Activity
     * recreation, and so a double tap can never open two billing sheets.
     */
    val purchaseInFlight: StateFlow<Boolean> = _purchaseInFlight.asStateFlow()

    private val _lastOutcome = MutableStateFlow<BillingOutcome?>(null)

    /**
     * The most recent finished purchase/restore, held until a UI reads it with
     * [consumeOutcome]. This is the guarantee that a user who paid always sees
     * *something*, even if the Activity was destroyed mid-purchase.
     */
    val lastOutcome: StateFlow<BillingOutcome?> = _lastOutcome.asStateFlow()

    /** Called by the paywall once it has rendered [lastOutcome]. */
    fun consumeOutcome() {
        _lastOutcome.value = null
    }

    /**
     * Configure the SDK against this install's stable id, so entitlements
     * follow the same identity the server meters the free allowance by.
     *
     * Safe to call more than once; the second call is a no-op.
     */
    fun configure(context: Context, appUserId: String, repository: PreferencesRepository) {
        preferencesRepository = repository
        appContext = context.applicationContext

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
            _purchasesSynced.value = true
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

        // Attach this Google Play account's existing purchases to this install
        // id. Without this, a subscriber who uninstalled (or switched phones)
        // looks free until they hunt for Restore — and we would show them the
        // paywall at the end of onboarding.
        //
        // syncPurchases, not restorePurchases: restore is the user-tapped
        // button (and can prompt a store sign-in). This is the silent launch
        // path. Offerings stay unfetched; PaywallScreen loads those on entry.
        scope.launch { syncPurchasesFromStore() }
    }

    /**
     * Suspend until [purchasesSynced] is true, or [STORE_SYNC_TIMEOUT_MS]
     * elapses. Callers that must not treat a subscriber as free — the
     * post-onboarding paywall, the keyboard's paywall intent — wait here.
     */
    suspend fun awaitPurchasesSynced() {
        if (_purchasesSynced.value) return
        withTimeoutOrNull(STORE_SYNC_WAIT_MS) {
            _purchasesSynced.first { it }
        }
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
        // Only announce "Loading" before the first successful load. A later
        // refresh — Activity.onResume fires one the instant the user comes back
        // from the Play billing sheet — must not yank an open paywall back to a
        // spinner and remount it mid-purchase.
        if (_offeringsState.value !is OfferingsState.Ready) {
            _offeringsState.value = OfferingsState.Loading
        }
        _offeringsState.value = try {
            OfferingsState.Ready(Purchases.sharedInstance.awaitOfferings().current)
        } catch (e: PurchasesException) {
            Log.e(TAG, "Could not load offerings: ${e.message}")
            // A failed *re*fresh is not a lost offering. Keep the plans already
            // on screen instead of replacing a usable paywall with an error the
            // user can do nothing about.
            _offeringsState.value as? OfferingsState.Ready ?: OfferingsState.Failed(e.message)
        }
    }

    /**
     * Start a purchase and publish its outcome on [lastOutcome].
     *
     * The work runs on the singleton's own scope, *not* the caller's: if
     * MainActivity is recreated while the Play billing sheet is up — a rotation,
     * memory pressure, or the keyboard's hand-off path — a composition-scoped
     * coroutine would be cancelled and the result of a real payment silently
     * dropped. Here it always lands, and the next paywall to compose reads it.
     *
     * [activity] is passed straight through to the suspend call as a parameter
     * and never stored in a field, so nothing outlives it.
     *
     * A second call while one is in flight is ignored, so a double tap cannot
     * open two billing sheets.
     */
    fun startPurchase(activity: Activity, packageToPurchase: Package) {
        start(BillingOperation.PURCHASE) { purchase(activity, packageToPurchase) }
    }

    /** Restore, on the same guarantees as [startPurchase]. */
    fun startRestore() {
        start(BillingOperation.RESTORE) { restorePurchases() }
    }

    private fun start(operation: BillingOperation, block: suspend () -> PurchaseResult) {
        // Atomic, so two taps landing in the same frame can't both get through.
        if (!_purchaseInFlight.compareAndSet(expect = false, update = true)) return
        _lastOutcome.value = null
        scope.launch {
            val result = try {
                block()
            } catch (e: Exception) {
                // block() already maps every RevenueCat error, so this only
                // catches the unforeseen — but leaving purchaseInFlight stuck
                // true would freeze the button for the rest of the process.
                Log.e(TAG, "$operation did not complete", e)
                PurchaseResult.Failed(
                    "Something went wrong. If you were charged, use Restore purchases."
                )
            }
            _purchaseInFlight.value = false
            _lastOutcome.value = BillingOutcome(operation, result)
        }
    }

    /**
     * Run the purchase flow. RevenueCat needs a real [Activity] to host the
     * billing sheet, which is why the keyboard hands the user off to
     * MainActivity instead of buying in-place.
     */
    private suspend fun purchase(activity: Activity, packageToPurchase: Package): PurchaseResult {
        if (!Purchases.isConfigured) {
            return PurchaseResult.Failed("Purchases aren't available right now")
        }
        return try {
            val result = Purchases.sharedInstance.awaitPurchase(
                PurchaseParams.Builder(activity, packageToPurchase).build()
            )
            onCustomerInfo(result.customerInfo)
            // Race: the CustomerInfo handed back here can predate the
            // entitlement. RevenueCat frequently delivers the entitlement a
            // beat later through the updatedCustomerInfoListener wired up in
            // configure(), so this single synchronous read says "not Pro" for a
            // purchase that actually succeeded. Don't fail on it alone — give
            // the entitlement a bounded moment to land first.
            if (result.customerInfo.hasPro() || awaitProEntitlement()) {
                PurchaseResult.Success
            } else {
                PurchaseResult.Failed(
                    "Purchase went through but $PRO_DISPLAY_NAME isn't active yet — " +
                        "it can take a moment. Try Restore purchases."
                )
            }
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) {
                PurchaseResult.Cancelled
            } else if (e.error.code == PurchasesErrorCode.ProductAlreadyPurchasedError) {
                // Same Play account already owns it — typical after reinstall
                // if they tap Subscribe instead of waiting for the launch sync.
                Log.i(TAG, "Play says this account already owns the product — syncing")
                restorePurchases()
            } else {
                Log.e(TAG, "Purchase failed: ${e.message}")
                PurchaseResult.Failed(e.message)
            }
        } catch (e: PurchasesException) {
            Log.e(TAG, "Purchase failed: ${e.message}")
            PurchaseResult.Failed(e.message)
        }
    }

    /**
     * Re-check the entitlement a few times for a purchase that came back
     * without it, returning true as soon as it shows up.
     *
     * Deliberately cache-busting ([CacheFetchPolicy.FETCH_CURRENT]): the cached
     * CustomerInfo is exactly what's stale in this situation. Every fetch still
     * goes through [onCustomerInfo] so the StateFlow and the persisted cache
     * move with it.
     *
     * A network error here isn't a purchase error — it only means we couldn't
     * confirm, so it ends the wait quietly instead of propagating.
     */
    private suspend fun awaitProEntitlement(): Boolean {
        repeat(ENTITLEMENT_RECHECK_ATTEMPTS) {
            delay(ENTITLEMENT_RECHECK_DELAY_MS)
            try {
                val customerInfo = Purchases.sharedInstance.awaitCustomerInfo(
                    fetchPolicy = CacheFetchPolicy.FETCH_CURRENT
                )
                onCustomerInfo(customerInfo)
                if (customerInfo.hasPro()) return true
            } catch (e: PurchasesException) {
                Log.w(TAG, "Could not re-check entitlement after purchase: ${e.message}")
                return false
            }
        }
        return false
    }

    /** Bring back a subscription bought on another install of the same account. */
    suspend fun restorePurchases(): PurchaseResult {
        return syncWithPlay(userInitiated = true)
    }

    /**
     * Ask Play for purchases on this Google account and attach them to this
     * install's RevenueCat user.
     *
     * Launch path uses [awaitSyncPurchases] so we never trigger a store
     * sign-in prompt. The Restore button uses [awaitRestore], which is the
     * user-initiated equivalent.
     */
    private suspend fun syncPurchasesFromStore() {
        syncWithPlay(userInitiated = false)
    }

    private suspend fun syncWithPlay(userInitiated: Boolean): PurchaseResult {
        if (!Purchases.isConfigured) {
            _purchasesSynced.value = true
            return PurchaseResult.Failed("Purchases aren't available right now")
        }

        return storeSyncMutex.withLock {
            try {
                val result = withTimeoutOrNull(STORE_SYNC_TIMEOUT_MS) {
                    try {
                        val customerInfo = if (userInitiated) {
                            Purchases.sharedInstance.awaitRestore()
                        } else {
                            Purchases.sharedInstance.awaitSyncPurchases()
                        }
                        onCustomerInfo(customerInfo)
                        if (customerInfo.hasPro()) {
                            PurchaseResult.Success
                        } else if (userInitiated) {
                            PurchaseResult.Failed("No active $PRO_DISPLAY_NAME subscription found")
                        } else {
                            // Launch sync found nothing. That's a real free user.
                            PurchaseResult.Cancelled
                        }
                    } catch (e: PurchasesTransactionException) {
                        if (e.userCancelled) {
                            PurchaseResult.Cancelled
                        } else {
                            Log.e(TAG, "Store sync failed: ${e.message}")
                            PurchaseResult.Failed(e.message)
                        }
                    } catch (e: PurchasesException) {
                        Log.e(TAG, "Store sync failed: ${e.message}")
                        PurchaseResult.Failed(e.message)
                    }
                }

                if (result == null) {
                    Log.w(TAG, "Store sync timed out after ${STORE_SYNC_TIMEOUT_MS}ms")
                    refreshCustomerInfo()
                } else if (result is PurchaseResult.Failed && !userInitiated) {
                    // Don't leave launch in a hole: fall back to whatever RC
                    // already cached for this install id.
                    refreshCustomerInfo()
                }

                result ?: PurchaseResult.Failed("Couldn't reach Google Play. Try Restore purchases.")
            } finally {
                _purchasesSynced.value = true
            }
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
        val becamePro = pro && !_isPro.value
        _isPro.value = pro

        // The one edge that turns a paid user into a blocked one: the server
        // caches entitlement lookups, so for up to a minute after this purchase
        // /generate-replies would still answer 402 to someone who just paid.
        // Every route that grants Pro — our own paywall, RevenueCat's hosted
        // paywall, Customer Center, the background listener — funnels through
        // here, so this is the one place that needs to nudge the server.
        if (becamePro) syncServerEntitlement()

        val repository = preferencesRepository ?: return
        scope.launch {
            runCatching { repository.updateIsPro(pro) }
                .onFailure { Log.w(TAG, "Could not cache entitlement", it) }
        }
    }

    /**
     * Best effort, and deliberately fire-and-forget: the purchase has already
     * succeeded, so a failure here must never reach the user as a purchase
     * error. Worst case the server catches up by itself when its cache expires.
     */
    private fun syncServerEntitlement() {
        val context = appContext ?: return
        scope.launch {
            // Off the main thread for the whole thing: building the HTTP stack
            // is part of the cost, and this scope runs on Main.immediate.
            runCatching {
                withContext(Dispatchers.IO) { ApiService(context).refreshServerEntitlement() }
            }.onFailure { Log.w(TAG, "Could not refresh the server entitlement", it) }
        }
    }
}

/**
 * The automatic paywall is only honest once Play has had a chance to attach
 * an existing subscription to this install. Showing it earlier sells Pro to
 * people who already pay.
 */
internal fun shouldPresentPaywall(isPro: Boolean, purchasesSynced: Boolean): Boolean =
    purchasesSynced && !isPro

