package com.tomlin7.l0v3.ui.screens.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tomlin7.l0v3.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageSettingsScreen(
    onNavigateBack: () -> Unit,
    preferencesRepository: PreferencesRepository,
    userPreferences: UserPreferences
) {
    val scope = rememberCoroutineScope()
    
    var selectedStyle by remember { mutableStateOf(userPreferences.style) }
    var selectedTone by remember { mutableStateOf(userPreferences.tone) }
    var selectedFlirtLevel by remember { mutableStateOf(userPreferences.flirtLevel) }
    var selectedReplyLength by remember { mutableStateOf(userPreferences.replyLength) }
    var selectedEmojiUse by remember { mutableStateOf(userPreferences.emojiUse) }
    
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
        // Top bar
        TopAppBar(
            title = { Text("Message Settings") },
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
            // Style
            SettingSection(
                title = "Style",
                description = "How your messages should look"
            ) {
                MessageStyle.entries.forEach { style ->
                    SettingOption(
                        text = style.displayName,
                        selected = selectedStyle == style,
                        onClick = {
                            selectedStyle = style
                            scope.launch {
                                preferencesRepository.updateStyle(style)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tone
            SettingSection(
                title = "Tone",
                description = "The vibe of your replies"
            ) {
                MessageTone.entries.forEach { tone ->
                    SettingOption(
                        text = tone.displayName,
                        selected = selectedTone == tone,
                        onClick = {
                            selectedTone = tone
                            scope.launch {
                                preferencesRepository.updateTone(tone)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Flirt Level
            SettingSection(
                title = "Flirt Level",
                description = "How bold to be"
            ) {
                FlirtLevel.entries.forEach { level ->
                    SettingOption(
                        text = level.displayName,
                        selected = selectedFlirtLevel == level,
                        onClick = {
                            selectedFlirtLevel = level
                            scope.launch {
                                preferencesRepository.updateFlirtLevel(level)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Reply Length
            SettingSection(
                title = "Reply Length",
                description = "How much to say"
            ) {
                ReplyLength.entries.forEach { length ->
                    SettingOption(
                        text = length.displayName,
                        selected = selectedReplyLength == length,
                        onClick = {
                            selectedReplyLength = length
                            scope.launch {
                                preferencesRepository.updateReplyLength(length)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Emoji Use
            SettingSection(
                title = "Emoji Use",
                description = "How expressive to be"
            ) {
                EmojiUse.entries.forEach { emojiUse ->
                    SettingOption(
                        text = emojiUse.displayName,
                        selected = selectedEmojiUse == emojiUse,
                        onClick = {
                            selectedEmojiUse = emojiUse
                            scope.launch {
                                preferencesRepository.updateEmojiUse(emojiUse)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingSection(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD946A6)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFD946A6).copy(alpha = 0.15f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color(0xFFD946A6)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFFD946A6) else Color.DarkGray
            )
        }
    }
}
