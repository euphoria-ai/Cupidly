package com.tomlin7.l0v3.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomlin7.l0v3.data.PreferencesRepository
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    preferencesRepository: PreferencesRepository
) {
    var currentPage by remember { mutableIntStateOf(0) }
    var apiKey by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
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
        when (currentPage) {
            0 -> WelcomePage(
                onNext = { currentPage++ }
            )
            1 -> ApiKeyPage(
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                onNext = {
                    scope.launch {
                        preferencesRepository.updateGeminiApiKey(apiKey)
                        currentPage++
                    }
                }
            )
            2 -> EnableKeyboardPage(
                context = context,
                onNext = {
                    scope.launch {
                        preferencesRepository.setOnboardingCompleted()
                        onComplete()
                    }
                }
            )
        }
    }
}

@Composable
fun WelcomePage(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❤️",
            style = MaterialTheme.typography.displayLarge
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to L0V3",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD946A6),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Type Less. Feel More.",
            style = MaterialTheme.typography.titleLarge,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Transform any chat into smooth, confident, and emotionally intelligent conversation powered by AI suggestions.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.DarkGray
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD946A6)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Get Started", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ApiKeyPage(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Key,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFD946A6)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Gemini API Key",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD946A6)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "L0V3 uses Google's Gemini AI to generate smart replies. You'll need an API key to get started.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.DarkGray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How to get your API key:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. Visit aistudio.google.com\n" +
                            "2. Sign in with your Google account\n" +
                            "3. Click 'Get API Key'\n" +
                            "4. Copy and paste it below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("API Key") },
            placeholder = { Text("AIza...") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD946A6),
                focusedLabelColor = Color(0xFFD946A6)
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = apiKey.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD946A6)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun EnableKeyboardPage(
    context: Context,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Keyboard,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFFD946A6)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Enable L0V3 Keyboard",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD946A6)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "To use L0V3, you need to enable it as an input method.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.DarkGray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        StepCard(
            number = "1",
            title = "Open Keyboard Settings",
            description = "Tap the button below to open system settings",
            icon = Icons.Default.Settings
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        StepCard(
            number = "2",
            title = "Enable L0V3 Keyboard",
            description = "Find 'L0V3 Keyboard' in the list and enable it",
            icon = Icons.Default.ToggleOn
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        StepCard(
            number = "3",
            title = "Select as Default",
            description = "Choose L0V3 as your default keyboard",
            icon = Icons.Default.Check
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        OutlinedButton(
            onClick = {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFFD946A6)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Open Settings", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD946A6)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Finish", style = MaterialTheme.typography.titleMedium)
        }
    }
}


@Composable
fun StepCard(
    number: String,
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
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
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
