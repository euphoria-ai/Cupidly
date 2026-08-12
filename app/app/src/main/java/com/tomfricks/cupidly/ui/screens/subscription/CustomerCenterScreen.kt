package com.tomfricks.cupidly.ui.screens.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.customercenter.CustomerCenterListener
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenterOptions
import com.tomfricks.cupidly.billing.BillingManager
import com.tomfricks.cupidly.ui.theme.PebbleButton
import com.tomfricks.cupidly.ui.theme.PebbleTone

/**
 * RevenueCat's Customer Center: the self-service screen where a subscriber can
 * see their plan, restore purchases, change it or cancel.
 *
 * Also the thing that satisfies Play's "users must be able to manage their
 * subscription from inside the app" expectation, without us hand-building a
 * management UI.
 *
 * Rendered only when the SDK is configured — with no API key there is no
 * customer to centre anything on, and the composable would just error out.
 */
@Composable
fun CustomerCenterScreen(
    onNavigateBack: () -> Unit
) {
    val isAvailable by BillingManager.isAvailable.collectAsState()

    val options = remember {
        CustomerCenterOptions.Builder()
            .setListener(
                object : CustomerCenterListener {
                    override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                        BillingManager.onCustomerInfo(customerInfo)
                    }
                }
            )
            .build()
    }

    // Cancelling or restoring can change the entitlement while this screen is
    // up; re-check on the way out so Home and the keyboard agree.
    DisposableEffect(Unit) {
        onDispose { BillingManager.refreshInBackground() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        if (isAvailable) {
            CustomerCenter(
                modifier = Modifier.fillMaxSize(),
                options = options,
                onDismiss = onNavigateBack
            )
        } else {
            // The nav back stack is restored after process death before the SDK
            // finishes configuring; rendering Customer Center then would throw.
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Subscription management isn't available right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                PebbleButton(
                    text = "Back",
                    onClick = onNavigateBack,
                    tone = PebbleTone.SLATE,
                    fillWidth = false
                )
            }
        }
    }
}
