package com.tomfricks.cupidly.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomfricks.cupidly.R
import com.tomfricks.cupidly.data.PreferencesRepository
import com.tomfricks.cupidly.data.ThemeMode
import com.tomfricks.cupidly.ui.theme.CupidlyTheme
import com.tomfricks.cupidly.ui.theme.PebbleButton
import com.tomfricks.cupidly.ui.theme.PebbleTone
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    preferencesRepository: PreferencesRepository
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Get current theme preference
    val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(
        initial = com.tomfricks.cupidly.data.UserPreferences()
    )
    
    CupidlyTheme(themeMode = userPreferences.themeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Page indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(2) { index ->
                val isActive = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isActive) 24.dp else 8.dp,
                    animationSpec = tween(300),
                    label = "indicator"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.3f,
                    animationSpec = tween(300),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                
                if (index < 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
        
        // Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> WelcomeSlide()
                1 -> EnableKeyboardSlide(context = context)
            }
        }
        
        // Navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pagerState.currentPage > 0) {
                PebbleButton(
                    text = "Back",
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    tone = PebbleTone.SLATE,
                    fillWidth = false
                )
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }
            
            PebbleButton(
                text = when (pagerState.currentPage) {
                    0 -> "Next"
                    1 -> "Finish"
                    else -> "Next"
                },
                onClick = {
                    when (pagerState.currentPage) {
                        0 -> {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        }
                        1 -> {
                            scope.launch {
                                preferencesRepository.setOnboardingCompleted()
                                onComplete()
                            }
                        }
                    }
                },
                fillWidth = false
            )
        }
        }
    }
}

@Composable
fun WelcomeSlide() {
    val isDarkTheme = isSystemInDarkTheme()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(
                id = if (isDarkTheme) R.drawable.heart_transparent
                     else R.drawable.heart_transparent_light
            ),
            contentDescription = "Hook",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Hook",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Type Less. Feel More.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun EnableKeyboardSlide(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Keyboard,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Enable Keyboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Activate Hook in your system settings",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        PebbleButton(
            text = "Open Settings",
            onClick = {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                context.startActivity(intent)
            }
        )
    }
}
