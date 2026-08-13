package com.tomfricks.hook.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomfricks.hook.HookApplication
import com.tomfricks.hook.billing.BillingManager
import com.tomfricks.hook.ui.screens.demo.DemoScreen
import com.tomfricks.hook.ui.screens.guide.GuideScreen
import com.tomfricks.hook.ui.screens.home.HomeScreen
import com.tomfricks.hook.ui.screens.onboarding.OnboardingScreen
import com.tomfricks.hook.ui.screens.onboarding.OnboardingSurvey
import com.tomfricks.hook.ui.screens.paywall.PaywallScreen
import com.tomfricks.hook.ui.screens.subscription.CustomerCenterScreen
import com.tomfricks.hook.ui.screens.welcome.WelcomeCarousel

sealed class Screen(val route: String) {
    /** The three pitch slides shown before any setup is asked for. */
    object Welcome : Screen("welcome")

    /** The profile questions — gender, sexuality, age, what they're after. */
    object Survey : Screen("survey")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Guide : Screen("guide")
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

    val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = com.tomfricks.hook.data.UserPreferences()
    )
    val isPro by BillingManager.isPro.collectAsState()

    val navController = rememberNavController()

    // First run opens on the pitch, not on a permission request: Welcome ->
    // Onboarding -> Home. Only Onboarding marks the flow complete, so quitting
    // mid-carousel starts over rather than skipping setup.
    val startDestination = if (userPreferences.hasCompletedOnboarding) {
        Screen.Home.route
    } else {
        Screen.Welcome.route
    }

    LaunchedEffect(paywallRequest) {
        if (paywallRequest > 0) {
            navController.navigate(Screen.Paywall.route)
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeCarousel(
                onFinished = { navController.navigate(Screen.Survey.route) }
            )
        }

        composable(Screen.Survey.route) {
            OnboardingSurvey(
                onFinished = {
                    navController.navigate(Screen.Onboarding.route) {
                        // The survey is answered and saved; going "back" into it
                        // from the keyboard step would only re-ask.
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                // Back out of the first question and you're on the last slide
                // again, which is where you came from.
                onBackFromStart = { navController.popBackStack() },
                preferencesRepository = preferencesRepository
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
                preferencesRepository = preferencesRepository
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToGuide = {
                    navController.navigate(Screen.Guide.route)
                },
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
                userPreferences = userPreferences,
                isPro = isPro
            )
        }

        composable(Screen.Guide.route) {
            GuideScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDemo = { navController.navigate(Screen.Demo.route) }
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
