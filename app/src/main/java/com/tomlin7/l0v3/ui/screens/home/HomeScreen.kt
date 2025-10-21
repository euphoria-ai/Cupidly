package com.tomlin7.l0v3.ui.screens.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.tomlin7.l0v3.R
import com.tomlin7.l0v3.data.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToGuide: () -> Unit,
    preferencesRepository: PreferencesRepository,
    userPreferences: UserPreferences
) {
    var currentPreferences by remember { mutableStateOf(userPreferences) }
    val coroutineScope = rememberCoroutineScope()
    
    // State for showing dropdowns
    var showStyleDropdown by remember { mutableStateOf(false) }
    var showToneDropdown by remember { mutableStateOf(false) }
    var showFlirtDropdown by remember { mutableStateOf(false) }
    var showLengthDropdown by remember { mutableStateOf(false) }
    var showEmojiDropdown by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }
    var showGenderDropdown by remember { mutableStateOf(false) }
    var showPronounsDropdown by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(userPreferences) {
        currentPreferences = userPreferences
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Icon - using theme-appropriate PNG
        val isDarkTheme = when (currentPreferences.themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }
        
        Image(
            painter = painterResource(
                id = if (isDarkTheme) R.drawable.heart_transparent 
                     else R.drawable.heart_transparent_light
            ),
            contentDescription = "L0V3",
            modifier = Modifier.size(120.dp),
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "L0V3",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Type Less. Feel More.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Getting Started Button
        PebbleButton(
            text = "How to Use",
            onClick = onNavigateToGuide
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Settings Section
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Style Settings
        SettingPebble(
            title = "Style",
            subtitle = currentPreferences.style.displayName,
            onClick = { showStyleDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingPebble(
            title = "Tone",
            subtitle = currentPreferences.tone.displayName,
            onClick = { showToneDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingPebble(
            title = "Flirt Level",
            subtitle = currentPreferences.flirtLevel.displayName,
            onClick = { showFlirtDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingPebble(
            title = "Reply Length",
            subtitle = currentPreferences.replyLength.displayName,
            onClick = { showLengthDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingPebble(
            title = "Emoji Use",
            subtitle = currentPreferences.emojiUse.displayName,
            onClick = { showEmojiDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingPebble(
            title = "Theme",
            subtitle = currentPreferences.themeMode.displayName,
            onClick = { showThemeDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Profile Section
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SettingPebble(
            title = "Gender",
            subtitle = currentPreferences.profileGender.ifEmpty { "Not set" },
            onClick = { showGenderDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingPebble(
            title = "Pronouns",
            subtitle = currentPreferences.profilePronouns.ifEmpty { "Not set" },
            onClick = { showPronounsDropdown = true }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        SettingPebble(
            title = "Bio",
            subtitle = currentPreferences.profileBio.ifEmpty { "Tell us about yourself" },
            onClick = { showBioDialog = true }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Reset Button
        PebbleButton(
            text = "Reset All Settings",
            onClick = { showResetDialog = true },
            isDestructive = true
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
    
    // Dropdown Dialogs
    if (showStyleDropdown) {
        SelectionDialog(
            title = "Select Style",
            options = MessageStyle.values().map { it.displayName },
            currentValue = currentPreferences.style.displayName,
            onDismiss = { showStyleDropdown = false },
            onSelect = { selectedText ->
                val newStyle = MessageStyle.values().find { it.displayName == selectedText } ?: currentPreferences.style
                val updated = currentPreferences.copy(style = newStyle)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showStyleDropdown = false
            }
        )
    }
    
    if (showToneDropdown) {
        SelectionDialog(
            title = "Select Tone",
            options = MessageTone.values().map { it.displayName },
            currentValue = currentPreferences.tone.displayName,
            onDismiss = { showToneDropdown = false },
            onSelect = { selectedText ->
                val newTone = MessageTone.values().find { it.displayName == selectedText } ?: currentPreferences.tone
                val updated = currentPreferences.copy(tone = newTone)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showToneDropdown = false
            }
        )
    }
    
    if (showFlirtDropdown) {
        SelectionDialog(
            title = "Select Flirt Level",
            options = FlirtLevel.values().map { it.displayName },
            currentValue = currentPreferences.flirtLevel.displayName,
            onDismiss = { showFlirtDropdown = false },
            onSelect = { selectedText ->
                val newFlirt = FlirtLevel.values().find { it.displayName == selectedText } ?: currentPreferences.flirtLevel
                val updated = currentPreferences.copy(flirtLevel = newFlirt)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showFlirtDropdown = false
            }
        )
    }
    
    if (showLengthDropdown) {
        SelectionDialog(
            title = "Select Reply Length",
            options = ReplyLength.values().map { it.displayName },
            currentValue = currentPreferences.replyLength.displayName,
            onDismiss = { showLengthDropdown = false },
            onSelect = { selectedText ->
                val newLength = ReplyLength.values().find { it.displayName == selectedText } ?: currentPreferences.replyLength
                val updated = currentPreferences.copy(replyLength = newLength)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showLengthDropdown = false
            }
        )
    }
    
    if (showEmojiDropdown) {
        SelectionDialog(
            title = "Select Emoji Use",
            options = EmojiUse.values().map { it.displayName },
            currentValue = currentPreferences.emojiUse.displayName,
            onDismiss = { showEmojiDropdown = false },
            onSelect = { selectedText ->
                val newEmoji = EmojiUse.values().find { it.displayName == selectedText } ?: currentPreferences.emojiUse
                val updated = currentPreferences.copy(emojiUse = newEmoji)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showEmojiDropdown = false
            }
        )
    }
    
    if (showThemeDropdown) {
        SelectionDialog(
            title = "Select Theme",
            options = ThemeMode.values().map { it.displayName },
            currentValue = currentPreferences.themeMode.displayName,
            onDismiss = { showThemeDropdown = false },
            onSelect = { selectedText ->
                val newTheme = ThemeMode.values().find { it.displayName == selectedText } ?: currentPreferences.themeMode
                val updated = currentPreferences.copy(themeMode = newTheme)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showThemeDropdown = false
            }
        )
    }
    
    if (showGenderDropdown) {
        SelectionDialog(
            title = "Select Gender",
            options = listOf("Male", "Female", "Non-binary", "Other", "Prefer not to say"),
            currentValue = currentPreferences.profileGender.ifEmpty { "Not set" },
            onDismiss = { showGenderDropdown = false },
            onSelect = { selectedText ->
                val updated = currentPreferences.copy(profileGender = selectedText)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showGenderDropdown = false
            }
        )
    }
    
    if (showPronounsDropdown) {
        SelectionDialog(
            title = "Select Pronouns",
            options = listOf("he/him", "she/her", "they/them", "ze/zir", "Other"),
            currentValue = currentPreferences.profilePronouns.ifEmpty { "Not set" },
            onDismiss = { showPronounsDropdown = false },
            onSelect = { selectedText ->
                val updated = currentPreferences.copy(profilePronouns = selectedText)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showPronounsDropdown = false
            }
        )
    }
    
    if (showBioDialog) {
        BioEditDialog(
            currentBio = currentPreferences.profileBio,
            onDismiss = { showBioDialog = false },
            onSave = { newBio ->
                val updated = currentPreferences.copy(profileBio = newBio)
                currentPreferences = updated
                coroutineScope.launch {
                    preferencesRepository.updateUserPreferences(updated)
                }
                showBioDialog = false
            }
        )
    }
    
    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This will reset all your preferences to default values. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            preferencesRepository.resetAllSettings()
                            currentPreferences = UserPreferences()
                        }
                        showResetDialog = false
                    }
                ) {
                    Text(
                        "Reset",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

@Composable
fun PebbleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
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
        targetValue = if (isPressed) 0.4f else 0.2f,
        animationSpec = tween(150),
        label = "glow"
    )
    
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val buttonColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 8.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = buttonColor.copy(alpha = glowAlpha * 0.3f),
                spotColor = buttonColor.copy(alpha = glowAlpha * 0.5f),
                clip = false
            )
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(
                            buttonColor.copy(alpha = 0.5f),
                            buttonColor.copy(alpha = 0.4f),
                            buttonColor.copy(alpha = 0.3f)
                        )
                    } else {
                        listOf(
                            buttonColor.copy(alpha = 0.7f),
                            buttonColor.copy(alpha = 0.6f),
                            buttonColor.copy(alpha = 0.5f)
                        )
                    }
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        buttonColor.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    radius = 300f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SettingPebble(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
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
        targetValue = if (isPressed) 0.2f else 0.05f,
        animationSpec = tween(150),
        label = "glow"
    )
    
    val isDarkTheme = MaterialTheme.colorScheme.background == Color(0xFF121212)
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = modifier
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
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (option == currentValue) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (option == currentValue) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
fun BioEditDialog(
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var bioText by remember { mutableStateOf(currentBio) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Bio",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            OutlinedTextField(
                value = bioText,
                onValueChange = { bioText = it },
                label = { Text("Tell us about yourself") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(bioText) }) {
                Text("Save", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}