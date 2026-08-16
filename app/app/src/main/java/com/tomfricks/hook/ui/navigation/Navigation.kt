package com.tomfricks.hook.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomfricks.hook.HookApplication
import com.tomfricks.hook.billing.BillingManager
import com.tomfricks.hook.ui.screens.demo.DemoScreen
import com.tomfricks.hook.ui.screens.home.HomeScreen
import com.tomfricks.hook.ui.screens.onboarding.OnboardingFlow
import com.tomfricks.hook.ui.screens.paywall.PaywallScreen
import com.tomfricks.hook.ui.screens.subscription.CustomerCenterScreen
import com.tomfricks.hook.ui.screens.welcome.WelcomeCarousel

sealed class Screen(val route: String) {
    /** The three pitch slides shown before any setup is asked for. */
    object Welcome : Screen("welcome")

    /** Everything from the profile questions to the keyboard being ready. */
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Demo : Screen("demo")
    object Paywall : Screen("paywall")
    object CustomerCenter : Screen("customer_center")
}

/**
 * @param paywallRequest a counter MainActivity bumps whenever something — today
 *   only the keyboard, which can't host a Play purchase sheet itself — asks for
 *   the paywall. Any increase navigates there.
 */
@Composable
fun HookNavigation(paywallRequest: Int = 0) {
    val context = LocalContext.current
    val app = context.applicationContext as HookApplication
    val preferencesRepository = app.preferencesRepository

    // Deliberately null until DataStore answers, rather than a default-valued
    // UserPreferences. A default says "onboarding not done", which would send
    // someone who finished months ago back to the first slide for a frame —
    // and NavHost fixes its start destination on first composition, so that
    // frame is not harmless.
    val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = null
    )
    val isPro by BillingManager.isPro.collectAsState()
    val entitlementSettled by BillingManager.entitlementSettled.collectAsState()

    val navController = rememberNavController()

    // Set when onboarding finishes, cleared once the paywall decision is made.
    // The decision is deferred rather than taken inline because on a reinstall
    // the entitlement comes back from Google Play a moment after launch, and
    // showing a paywall to someone who is already paying is the worst version
    // of this screen.
    var paywallAfterOnboarding by rememberSaveable { mutableStateOf(false) }

    val preferences = userPreferences
    if (preferences == null) {
        // One frame of the app's own background while the answer arrives.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    // First run opens on the pitch, not on a permission request: Welcome ->
    // Onboarding -> Home. Onboarding is what marks the flow complete, and it is
    // never shown again once it has.
    val startDestination = if (preferences.hasCompletedOnboarding) {
        Screen.Home.route
    } else {
        Screen.Welcome.route
    }

    LaunchedEffect(paywallRequest) {
        if (paywallRequest > 0) {
            navController.navigate(Screen.Paywall.route)
        }
    }

    LaunchedEffect(paywallAfterOnboarding, entitlementSettled, isPro) {
        if (!paywallAfterOnboarding) return@LaunchedEffect
        // isPro landing early is an answer in itself; otherwise wait for the
        // launch-time resolution to have asked everyone, Play included.
        if (!isPro && !entitlementSettled) return@LaunchedEffect
        paywallAfterOnboarding = false
        if (!isPro) {
            navController.navigate(Screen.Paywall.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeCarousel(
                onFinished = { navController.navigate(Screen.Onboarding.route) }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingFlow(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        // Onboarding is recorded as done; there is nothing to
                        // come back to.
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                    // They have just been shown three Pro answers they weren't
                    // allowed to pick. Ask for the upgrade here, while that's
                    // still fresh — and push it on top of Home, so closing it
                    // lands them in the app rather than back in onboarding.
                    paywallAfterOnboarding = true
                },
                // Back out of the first question and you're on the last pitch
                // slide again, which is where you came from.
                onBackFromStart = { navController.popBackStack() },
                preferencesRepository = preferencesRepository,
                isPro = isPro
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDemo = {
                    navController.navigate(Screen.Demo.route)
                },
                onNavigateToPaywall = {
                    navController.navigate(Screen.Paywall.route)
                },
                onNavigateToCustomerCenter = {
                    navController.navigate(Screen.CustomerCenter.route)
                },
                preferencesRepository = preferencesRepository,
                userPreferences = preferences,
                isPro = isPro
            )
        }

        composable(Screen.Demo.route) {
            DemoScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Paywall.route) {
            PaywallScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerCenter.route) {
            CustomerCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
