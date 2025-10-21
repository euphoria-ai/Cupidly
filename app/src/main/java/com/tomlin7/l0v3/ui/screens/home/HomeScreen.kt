package com.tomlin7.l0v3.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomlin7.l0v3.data.UserPreferences

@Composable
fun HomeScreen(
    onNavigateToMessageSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    userPreferences: UserPreferences
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF0F5),
                        Color(0xFFFFE4E1),
                        Color(0xFFFFF5EE)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "❤️",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "L0V3",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD946A6)
        )
        
        Text(
            text = "Type Less. Feel More.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Quick Start",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD946A6)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "1. Open any chat app\n" +
                            "2. Take a screenshot (Power + Vol Down)\n" +
                            "3. Switch to L0V3 Keyboard\n" +
                            "4. Tap ❤️ for AI reply suggestions\n" +
                            "5. Choose and send a reply",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        
        SettingsCard(
            icon = Icons.Default.Message,
            title = "Message Settings",
            description = "Customize tone, style, and reply preferences",
            onClick = onNavigateToMessageSettings
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsCard(
            icon = Icons.Default.Person,
            title = "Profile",
            description = "Personalize your AI assistant",
            onClick = onNavigateToProfile
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingsCard(
            icon = Icons.Default.Security,
            title = "Privacy",
            description = "Learn how we protect your data",
            onClick = onNavigateToPrivacy
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Current preferences preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFD946A6).copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Settings",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD946A6)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Style: ${userPreferences.style.displayName} • " +
                            "Tone: ${userPreferences.tone.displayName} • " +
                            "Flirt: ${userPreferences.flirtLevel.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFD946A6).copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFD946A6),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray
            )
        }
    }
}
