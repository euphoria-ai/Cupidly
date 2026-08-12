package com.tomfricks.cupidly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tomfricks.cupidly.billing.BillingManager
import com.tomfricks.cupidly.data.UserPreferences
import com.tomfricks.cupidly.ui.navigation.CupidlyNavigation
import com.tomfricks.cupidly.ui.theme.CupidlyTheme

class MainActivity : ComponentActivity() {

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission handled, nothing special needed
    }

    /**
     * Bumped every time something asks for the paywall. A counter rather than a
     * flag so a second request while the app is already open still navigates.
     */
    private var paywallRequest by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permission for screenshot detection
        requestStoragePermissionIfNeeded()

        handlePaywallIntent(intent)

        setContent {
            val app = application as CupidlyApplication
            val userPreferences by app.preferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences()
            )

            CupidlyTheme(themeMode = userPreferences.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CupidlyNavigation(paywallRequest = paywallRequest)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // The keyboard launches us with FLAG_ACTIVITY_NEW_TASK while the app is
        // often already alive; singleTop routes that here instead of onCreate.
        setIntent(intent)
        handlePaywallIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // A subscription can be bought, cancelled or refunded outside the app.
        BillingManager.refreshCustomerInfo()
    }

    /** The keyboard can't host a Play purchase sheet, so it sends the user here. */
    private fun handlePaywallIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_SHOW_PAYWALL, false) == true) {
            paywallRequest++
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(permission)
        }
    }

    companion object {
        /** Boolean extra: open straight onto the paywall route. */
        const val EXTRA_SHOW_PAYWALL = "com.tomfricks.cupidly.extra.SHOW_PAYWALL"
    }
}
