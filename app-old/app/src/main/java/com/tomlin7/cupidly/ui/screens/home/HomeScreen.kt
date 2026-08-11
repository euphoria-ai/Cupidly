package com.tomlin7.cupidly.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tomlin7.cupidly.R
import com.tomlin7.cupidly.data.EmojiUse
import com.tomlin7.cupidly.data.FlirtLevel
import com.tomlin7.cupidly.data.MessageStyle
import com.tomlin7.cupidly.data.MessageTone
import com.tomlin7.cupidly.data.PreferencesRepository
import com.tomlin7.cupidly.data.ReplyLength
import com.tomlin7.cupidly.data.ThemeMode
import com.tomlin7.cupidly.data.UserPreferences
import com.tomlin7.cupidly.ui.theme.PebbleButton
import com.tomlin7.cupidly.ui.theme.PebbleRow
import com.tomlin7.cupidly.ui.theme.PebbleTone
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToGuide: () -> Unit,
    onNavigateToDemo: () -> Unit,
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
            contentDescription = "Cupidly",
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Cupidly",
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

        PebbleButton(
            text = "Try the demo",
            onClick = onNavigateToDemo
        )

        Spacer(modifier = Modifier.height(12.dp))

        PebbleButton(
            text = "How to Use",
            onClick = onNavigateToGuide,
            tone = PebbleTone.SLATE
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
            tone = PebbleTone.DANGER
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
                val newStyle = MessageStyle.values().find { it.displayName == selectedText }
                    ?: currentPreferences.style
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
                val newTone = MessageTone.values().find { it.displayName == selectedText }
                    ?: currentPreferences.tone
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
                val newFlirt = FlirtLevel.values().find { it.displayName == selectedText }
                    ?: currentPreferences.flirtLevel
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
                val newLength = ReplyLength.values().find { it.displayName == selectedText }
                    ?: currentPreferences.replyLength
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
                val newEmoji = EmojiUse.values().find { it.displayName == selectedText }
                    ?: currentPreferences.emojiUse
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
                val newTheme = ThemeMode.values().find { it.displayName == selectedText }
                    ?: currentPreferences.themeMode
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
private fun SettingPebble(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PebbleRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        modifier = modifier,
        trailingIcon = Icons.Default.ChevronRight
    )
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
