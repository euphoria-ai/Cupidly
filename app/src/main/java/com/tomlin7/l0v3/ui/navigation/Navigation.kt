package com.tomlin7.l0v3.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomlin7.l0v3.L0V3Application
import com.tomlin7.l0v3.ui.screens.guide.GuideScreen
import com.tomlin7.l0v3.ui.screens.home.HomeScreen
import com.tomlin7.l0v3.ui.screens.onboarding.OnboardingScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Guide : Screen("guide")
}

@Composable
fun L0V3Navigation() {
    val context = LocalContext.current
    val app = context.applicationContext as L0V3Application
    val preferencesRepository = app.preferencesRepository
    
    val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = com.tomlin7.l0v3.data.UserPreferences()
    )
    
    val navController = rememberNavController()
    
    val startDestination = if (userPreferences.hasCompletedOnboarding) {
        Screen.Home.route
    } else {
        Screen.Onboarding.route
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
                preferencesRepository = preferencesRepository,
                userPreferences = userPreferences
            )
        }
        
        composable(Screen.Guide.route) {
            GuideScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
