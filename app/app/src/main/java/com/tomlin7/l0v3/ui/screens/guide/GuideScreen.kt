package com.tomlin7.l0v3.ui.screens.guide

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.tomlin7.l0v3.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = "Guide",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            val isDarkTheme = isSystemInDarkTheme()
            Image(
                painter = painterResource(
                    id = if (isDarkTheme) R.drawable.heart_transparent 
                         else R.drawable.heart_transparent_light
                ),
                contentDescription = "L0V3",
                modifier = Modifier.size(100.dp),
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "How to Use",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Simple steps to get started",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Steps
            val steps = listOf(
                "Open your messaging app",
                "Switch to L0V3 keyboard",
                "Take a screenshot of the chat",
                "Pick to send your favorite",
                "Generate more rizz!"
            )
            
            steps.forEachIndexed { index, step ->
                GuideStep(
                    stepNumber = (index + 1).toString(),
                    description = step
                )
                
                if (index < steps.size - 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Pro Tips
            Text(
                text = "Pro Tips (from me)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val tips = listOf(
                "Customize to control the rizz",
                "Complete your bio for more rizz",
                "Try with recent messages",
            )
            
            tips.forEach { tip ->
                PebbleTip(text = tip)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GuideStep(
    stepNumber: String,
    description: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animation for press effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.3f else 0.1f,
        animationSpec = tween(150),
        label = "glow"
    )
    
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 6.dp else 3.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = primaryColor.copy(alpha = glowAlpha * 0.3f),
                spotColor = primaryColor.copy(alpha = glowAlpha * 0.5f),
                clip = false
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            primaryColor.copy(alpha = 0.4f),
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor.copy(alpha = 0.2f)
                        )
                    } else {
                        listOf(
                            primaryColor.copy(alpha = 0.6f),
                            primaryColor.copy(alpha = 0.5f),
                            primaryColor.copy(alpha = 0.4f)
                        )
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    radius = 200f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Description
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PebbleTip(text: String) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Animation for press effect
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val glowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.2f else 0.05f,
        animationSpec = tween(150),
        label = "glow"
    )
    
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 4.dp else 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = surfaceColor.copy(alpha = glowAlpha * 0.3f),
                spotColor = surfaceColor.copy(alpha = glowAlpha * 0.5f),
                clip = false
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            surfaceColor.copy(alpha = 0.3f),
                            surfaceColor.copy(alpha = 0.2f),
                            surfaceColor.copy(alpha = 0.1f)
                        )
                    } else {
                        listOf(
                            surfaceColor.copy(alpha = 0.5f),
                            surfaceColor.copy(alpha = 0.4f),
                            surfaceColor.copy(alpha = 0.3f)
                        )
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { }
            )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}