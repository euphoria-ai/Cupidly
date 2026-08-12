package com.tomfricks.cupidly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomfricks.cupidly.CupidlyApplication
import com.tomfricks.cupidly.billing.BillingManager
import com.tomfricks.cupidly.ui.screens.demo.DemoScreen
import com.tomfricks.cupidly.ui.screens.guide.GuideScreen
import com.tomfricks.cupidly.ui.screens.home.HomeScreen
import com.tomfricks.cupidly.ui.screens.onboarding.OnboardingScreen
import com.tomfricks.cupidly.ui.screens.paywall.PaywallScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Guide : Screen("guide")
    object Demo : Screen("demo")
    object Paywall : Screen("paywall")
}

/**
 * @param paywallRequest a counter MainActivity bumps whenever something — today
 *   only the keyboard, which can't host a Play purchase sheet itself — asks for
 *   the paywall. Any increase navigates there.
 */
@Composable
fun CupidlyNavigation(paywallRequest: Int = 0) {
    val context = LocalContext.current
    val app = context.applicationContext as CupidlyApplication
    val preferencesRepository = app.preferencesRepository

    val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = com.tomfricks.cupidly.data.UserPreferences()
    )
    val isPro by BillingManager.isPro.collectAsState()

    val navController = rememberNavController()

    val startDestination = if (userPreferences.hasCompletedOnboarding) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
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
    }
}
