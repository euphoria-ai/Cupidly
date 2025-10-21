package com.tomlin7.l0v3.ui.screens.privacy

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onNavigateBack: () -> Unit) {
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
    ) {
        TopAppBar(
            title = { Text("Privacy") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color(0xFFD946A6)
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFD946A6).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFD946A6),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Your privacy is our priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD946A6)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "How L0V3 Works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD946A6)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            PrivacyCard(
                icon = Icons.Default.Screenshot,
                title = "Screenshot Detection",
                description = "YOU take screenshots using your device's screenshot button (Power + Volume Down). L0V3 detects when you take a screenshot and uses it to generate replies when you tap ❤️. L0V3 never captures screenshots automatically."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PrivacyCard(
                icon = Icons.Default.Cloud,
                title = "AI Processing",
                description = "Screenshots are sent to Google's Gemini API for analysis and reply generation. This happens over a secure connection. Gemini processes the image and returns text suggestions."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PrivacyCard(
                icon = Icons.Default.Delete,
                title = "Data Processing",
                description = "L0V3 only reads the screenshot file you manually captured. After AI replies are generated, the screenshot data is discarded from memory. The original screenshot file remains on your device until you delete it. We don't store conversations or personal data."
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PrivacyCard(
                icon = Icons.Default.Lock,
                title = "Local Storage",
                description = "Only your preferences (style, tone, profile info) are stored locally on your device. Your Gemini API key is stored securely and never leaves your device except to authenticate with Gemini."
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "What We Don't Do",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD946A6)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    PrivacyListItem("❌ We don't store your conversations")
                    PrivacyListItem("❌ We don't track your typing habits")
                    PrivacyListItem("❌ We don't share data with third parties")
                    PrivacyListItem("❌ We don't use your data for advertising")
                    PrivacyListItem("❌ We don't access your contacts or files")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Your Control",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD946A6)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "• YOU take screenshots manually using device buttons\n" +
                                "• L0V3 only processes screenshots YOU capture\n" +
                                "• No automatic screenshot capturing ever\n" +
                                "• You decide which chats to screenshot\n" +
                                "• Delete screenshots from your gallery anytime\n" +
                                "• Delete the app and all data anytime",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PrivacyCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
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
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFD946A6)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }
    }
}

@Composable
fun PrivacyListItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray
        )
    }
}
