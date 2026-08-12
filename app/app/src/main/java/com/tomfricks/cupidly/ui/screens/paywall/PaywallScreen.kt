package com.tomfricks.cupidly.ui.screens.paywall

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.tomfricks.cupidly.billing.BillingManager
import com.tomfricks.cupidly.billing.OfferingsState
import com.tomfricks.cupidly.billing.PurchaseResult
import com.tomfricks.cupidly.ui.theme.PebbleButton
import com.tomfricks.cupidly.ui.theme.PebbleIconButton
import com.tomfricks.cupidly.ui.theme.PebbleSurface
import com.tomfricks.cupidly.ui.theme.PebbleTextButton
import com.tomfricks.cupidly.ui.theme.PebbleTone
import kotlinx.coroutines.launch

private const val TERMS_URL = "https://cupidly.app/terms"
private const val PRIVACY_URL = "https://cupidly.app/privacy"

private val PRO_BENEFITS = listOf(
    "Unlimited rizz — no more free-generation counter",
    "Every tone, including Gen-Z Slang and Respectful",
    "Bold flirt level and Extended replies",
    "Priority on new Hook features"
)

/**
 * The paywall route.
 *
 * Prefers RevenueCat's dashboard-configured Paywall so pricing, copy and layout
 * can be changed without shipping an app update. Falls back to the hand-rolled
 * Pebble paywall when there is nothing to render remotely — no current offering,
 * no paywall attached to it, a failed fetch, or no SDK key at all — because a
 * blank screen at the moment of purchase is the worst possible outcome.
 */
@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit
) {
    val isAvailable by BillingManager.isAvailable.collectAsState()
    val offeringsState by BillingManager.offeringsState.collectAsState()

    LaunchedEffect(Unit) {
        BillingManager.refreshCustomerInfo()
        BillingManager.refreshOfferings()
    }

    // Only the dashboard-configured offering can drive the hosted paywall; an
    // offering without a paywall attached would render an error dialog.
    val hostedOffering = (offeringsState as? OfferingsState.Ready)
        ?.offering
        ?.takeIf { it.hasPaywall }

    when {
        !isAvailable -> FallbackPaywall(onNavigateBack = onNavigateBack)

        offeringsState is OfferingsState.Idle || offeringsState is OfferingsState.Loading ->
            PaywallLoading()

        hostedOffering != null -> HostedPaywall(
            offering = hostedOffering,
            onDismiss = onNavigateBack
        )

        else -> FallbackPaywall(onNavigateBack = onNavigateBack)
    }
}

/** RevenueCat's templated paywall, configured entirely from the dashboard. */
@Composable
private fun HostedPaywall(
    offering: Offering,
    onDismiss: () -> Unit
) {
    // The SDK calls dismissRequest on close *and* after a successful purchase or
    // restore, so this is also the "we're done here" hook.
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    val options = remember(offering) {
        PaywallOptions.Builder {
            BillingManager.refreshInBackground()
            currentOnDismiss()
        }
            .setOffering(offering)
            .setShouldDisplayDismissButton(true)
            .setListener(
                object : PaywallListener {
                    override fun onPurchaseCompleted(
                        customerInfo: CustomerInfo,
                        storeTransaction: StoreTransaction
                    ) {
                        BillingManager.onCustomerInfo(customerInfo)
                    }

                    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                        BillingManager.onCustomerInfo(customerInfo)
                    }
                }
            )
            .build()
    }

    Paywall(options)
}

@Composable
private fun PaywallLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Hand-rolled Hook Pro paywall on the Pebble design system.
 *
 * The safety net for when RevenueCat has no paywall to give us. Packages,
 * prices and the purchase flow still come straight from the SDK — nothing about
 * the plan line-up is hardcoded, it renders whatever the current offering says.
 */
@Composable
private fun FallbackPaywall(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    val isPro by BillingManager.isPro.collectAsState()
    val offeringsState by BillingManager.offeringsState.collectAsState()
    val isAvailable by BillingManager.isAvailable.collectAsState()

    var selectedPackage by remember { mutableStateOf<Package?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val isLoadingOfferings =
        offeringsState is OfferingsState.Idle || offeringsState is OfferingsState.Loading

    // Whatever the dashboard's current offering contains, ordered shortest
    // period first (weekly → monthly → yearly) rather than by a fixed list.
    val packages = remember(offeringsState) {
        (offeringsState as? OfferingsState.Ready)
            ?.offering
            ?.availablePackages
            .orEmpty()
            .sortedBy { it.planRank() }
    }
    val bestValue = remember(packages) {
        if (packages.size > 1) packages.maxByOrNull { it.planRank() } else null
    }

    LaunchedEffect(packages) {
        if (selectedPackage == null || packages.none { it.identifier == selectedPackage?.identifier }) {
            selectedPackage = bestValue ?: packages.firstOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            PebbleIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                onClick = onNavigateBack
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = BillingManager.PRO_DISPLAY_NAME,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Unlimited rizz. Every tone. Every reply.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        PRO_BENEFITS.forEach { benefit ->
            BenefitRow(text = benefit)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        when {
            isPro -> {
                Text(
                    text = "You're on ${BillingManager.PRO_DISPLAY_NAME}. Thanks for the support.",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                PebbleButton(
                    text = "Done",
                    onClick = onNavigateBack,
                    tone = PebbleTone.SLATE
                )
            }

            !isAvailable -> {
                Text(
                    text = "Subscriptions aren't available in this build yet. " +
                        "Check back after the next update.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            packages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingOfferings) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(
                            text = "Couldn't load the plans. Check your connection and try again.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                packages.forEach { availablePackage ->
                    PackageRow(
                        availablePackage = availablePackage,
                        selected = availablePackage.identifier == selectedPackage?.identifier,
                        isBestValue = availablePackage.identifier == bestValue?.identifier,
                        onClick = { selectedPackage = availablePackage }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                val chosen = selectedPackage
                PebbleButton(
                    text = when {
                        isWorking -> "Working…"
                        chosen != null && chosen.hasFreeTrial() -> "Start free trial"
                        else -> "Continue"
                    },
                    onClick = {
                        if (isWorking) return@PebbleButton
                        if (activity == null || chosen == null) {
                            statusMessage = "Couldn't start the purchase. Try again."
                            return@PebbleButton
                        }
                        isWorking = true
                        statusMessage = null
                        coroutineScope.launch {
                            val result = BillingManager.purchase(activity, chosen)
                            isWorking = false
                            statusMessage = when (result) {
                                is PurchaseResult.Success ->
                                    "You're on ${BillingManager.PRO_DISPLAY_NAME}. Enjoy."
                                is PurchaseResult.Cancelled -> null
                                is PurchaseResult.Failed -> result.message
                            }
                        }
                    }
                )
            }
        }

        val message = statusMessage
        if (message != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Google Play requires the renewal terms to be visible on the screen
        // where the subscription is offered.
        Text(
            text = billingDisclosure(selectedPackage),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PebbleTextButton(
                text = "Restore purchases",
                onClick = {
                    if (isWorking) return@PebbleTextButton
                    isWorking = true
                    statusMessage = null
                    coroutineScope.launch {
                        val result = BillingManager.restorePurchases()
                        isWorking = false
                        statusMessage = when (result) {
                            is PurchaseResult.Success ->
                                "${BillingManager.PRO_DISPLAY_NAME} restored."
                            is PurchaseResult.Cancelled -> null
                            is PurchaseResult.Failed -> result.message
                        }
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            PebbleTextButton(
                text = "Terms",
                onClick = { uriHandler.openUri(TERMS_URL) },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PebbleTextButton(
                text = "Privacy",
                onClick = { uriHandler.openUri(PRIVACY_URL) },
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BenefitRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** One selectable plan: period on the left, localised store price on the right. */
@Composable
private fun PackageRow(
    availablePackage: Package,
    selected: Boolean,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val trial = availablePackage.hasFreeTrial()

    PebbleSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        tone = if (selected) PebbleTone.BLUE else PebbleTone.MUTED,
        cornerRadius = 18.dp,
        elevation = if (selected) 5.dp else 2.dp,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = availablePackage.planTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                val caption = when {
                    trial && isBestValue -> "Best value · free trial included"
                    isBestValue -> "Best value"
                    trial -> "Free trial included"
                    else -> null
                }
                if (caption != null) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                }
            }
            Text(
                text = availablePackage.product.price.formatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/** True when the product's default subscription option opens with a free phase. */
private fun Package.hasFreeTrial(): Boolean = product.defaultOption?.freePhase != null

/**
 * Plans are keyed off [PackageType] when the dashboard uses RevenueCat's
 * predefined identifiers, and off the identifier text when it uses custom ones
 * (our dashboard's offering uses plain `weekly` / `monthly` / `yearly`, which
 * arrive as [PackageType.CUSTOM]).
 *
 * Multi-month variants are matched before plain monthly so "two_month" doesn't
 * get swallowed by the "month" test.
 */
private fun Package.planKey(): PlanKey {
    val id = identifier.lowercase()
    return when {
        packageType == PackageType.LIFETIME || id.contains("lifetime") -> PlanKey.LIFETIME
        packageType == PackageType.WEEKLY || id.contains("week") -> PlanKey.WEEKLY
        packageType == PackageType.TWO_MONTH || id.contains("two_month") || id.contains("2month") ->
            PlanKey.TWO_MONTH
        packageType == PackageType.THREE_MONTH || id.contains("three_month") || id.contains("3month") ->
            PlanKey.THREE_MONTH
        packageType == PackageType.SIX_MONTH || id.contains("six_month") || id.contains("6month") ->
            PlanKey.SIX_MONTH
        packageType == PackageType.ANNUAL || id.contains("year") || id.contains("annual") ->
            PlanKey.ANNUAL
        packageType == PackageType.MONTHLY || id.contains("month") -> PlanKey.MONTHLY
        else -> PlanKey.OTHER
    }
}

private fun Package.planRank(): Int = planKey().rank

private fun Package.planTitle(): String = planKey().title

/** Shortest period first, so the list reads weekly → monthly → yearly. */
private enum class PlanKey(val rank: Int, val title: String, val periodLabel: String?) {
    WEEKLY(0, "Weekly", "week"),
    MONTHLY(1, "Monthly", "month"),
    TWO_MONTH(2, "Every 2 months", "2 months"),
    THREE_MONTH(3, "Every 3 months", "3 months"),
    SIX_MONTH(4, "Every 6 months", "6 months"),
    ANNUAL(5, "Yearly", "year"),
    LIFETIME(6, "Lifetime", null),
    OTHER(7, BillingManager.PRO_DISPLAY_NAME, "billing period")
}

/**
 * The Play-policy disclosure: what it costs, how often it bills, that it keeps
 * renewing, and how to stop it.
 */
private fun billingDisclosure(selected: Package?): String {
    val pro = BillingManager.PRO_DISPLAY_NAME
    if (selected == null) {
        return "$pro is an auto-renewing subscription billed through Google Play. " +
            "It renews automatically until you cancel it in Google Play › Subscriptions."
    }

    val price = selected.product.price.formatted
    val key = selected.planKey()
    if (key == PlanKey.LIFETIME) {
        return "$pro Lifetime is a one-time purchase of $price through Google Play. " +
            "It does not renew."
    }

    val period = key.periodLabel ?: "billing period"
    val trialSentence = if (selected.hasFreeTrial()) {
        "Your free trial converts to a paid subscription unless you cancel before it ends. "
    } else {
        ""
    }

    return "$pro is an auto-renewing subscription. $trialSentence" +
        "You'll be charged $price per $period through Google Play. It renews automatically " +
        "for the same price and period until you cancel, at least 24 hours before the current " +
        "period ends, in Google Play › Subscriptions."
}

/** Walk the Compose context chain to the hosting Activity — RevenueCat needs one. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
