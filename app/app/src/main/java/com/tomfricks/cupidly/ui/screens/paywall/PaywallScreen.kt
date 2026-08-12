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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.tomfricks.cupidly.billing.BillingManager
import com.tomfricks.cupidly.billing.PurchaseResult
import com.tomfricks.cupidly.ui.theme.PebbleButton
import com.tomfricks.cupidly.ui.theme.PebbleIconButton
import com.tomfricks.cupidly.ui.theme.PebbleSurface
import com.tomfricks.cupidly.ui.theme.PebbleTextButton
import com.tomfricks.cupidly.ui.theme.PebbleTone

private const val TERMS_URL = "https://cupidly.app/terms"
private const val PRIVACY_URL = "https://cupidly.app/privacy"

private val PRO_BENEFITS = listOf(
    "Unlimited rizz — no more free-generation counter",
    "Every tone, including Gen-Z Slang and Respectful",
    "Bold flirt level and Extended replies",
    "Priority on new Hook features"
)

/**
 * Hook Pro paywall.
 *
 * Hand-rolled on the Pebble design system rather than RevenueCat's prebuilt
 * paywall so it matches the rest of the app and stays under our control; the
 * packages, prices and purchase flow still come straight from RevenueCat.
 */
@Composable
fun PaywallScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uriHandler = LocalUriHandler.current

    val isPro by BillingManager.isPro.collectAsState()
    val offering by BillingManager.currentOffering.collectAsState()
    val isLoadingOfferings by BillingManager.isLoadingOfferings.collectAsState()
    val isAvailable by BillingManager.isAvailable.collectAsState()

    var selectedPackage by remember { mutableStateOf<Package?>(null) }
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        BillingManager.refreshOfferings()
        BillingManager.refreshCustomerInfo()
    }

    val packages = offering?.availablePackages.orEmpty()

    // Default to the annual plan when there is one — best value, and it is what
    // the disclosure copy below describes until the user picks something else.
    LaunchedEffect(packages) {
        if (selectedPackage == null || packages.none { it.identifier == selectedPackage?.identifier }) {
            selectedPackage = packages.firstOrNull { it.packageType == PackageType.ANNUAL }
                ?: packages.firstOrNull()
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
            text = "Hook Pro",
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
                    text = "You're on Hook Pro. Thanks for the support.",
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
                        BillingManager.purchase(activity, chosen) { result ->
                            isWorking = false
                            statusMessage = when (result) {
                                is PurchaseResult.Success -> "You're on Hook Pro. Enjoy."
                                is PurchaseResult.Cancelled -> null
                                is PurchaseResult.Failed -> result.message
                            }
                        }
                    }
                )
            }
        }

        if (statusMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = statusMessage!!,
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
                    BillingManager.restorePurchases { result ->
                        isWorking = false
                        statusMessage = when (result) {
                            is PurchaseResult.Success -> "Hook Pro restored."
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
                    text = planTitle(availablePackage.packageType),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                if (trial) {
                    Text(
                        text = "Free trial included",
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

private fun planTitle(packageType: PackageType): String = when (packageType) {
    PackageType.WEEKLY -> "Weekly"
    PackageType.MONTHLY -> "Monthly"
    PackageType.TWO_MONTH -> "Every 2 months"
    PackageType.THREE_MONTH -> "Every 3 months"
    PackageType.SIX_MONTH -> "Every 6 months"
    PackageType.ANNUAL -> "Annual"
    PackageType.LIFETIME -> "Lifetime"
    else -> "Hook Pro"
}

private fun billingPeriodLabel(packageType: PackageType): String = when (packageType) {
    PackageType.WEEKLY -> "week"
    PackageType.MONTHLY -> "month"
    PackageType.TWO_MONTH -> "2 months"
    PackageType.THREE_MONTH -> "3 months"
    PackageType.SIX_MONTH -> "6 months"
    PackageType.ANNUAL -> "year"
    else -> "billing period"
}

/**
 * The Play-policy disclosure: what it costs, how often it bills, that it keeps
 * renewing, and how to stop it.
 */
private fun billingDisclosure(selected: Package?): String {
    if (selected == null) {
        return "Hook Pro is an auto-renewing subscription billed through Google Play. " +
            "It renews automatically until you cancel it in Google Play › Subscriptions."
    }
    if (selected.packageType == PackageType.LIFETIME) {
        return "Hook Pro Lifetime is a one-time purchase of " +
            "${selected.product.price.formatted} through Google Play. It does not renew."
    }

    val price = selected.product.price.formatted
    val period = billingPeriodLabel(selected.packageType)
    val trialSentence = if (selected.hasFreeTrial()) {
        "Your free trial converts to a paid subscription unless you cancel before it ends. "
    } else {
        ""
    }

    return "Hook Pro is an auto-renewing subscription. $trialSentence" +
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
