package com.tomfricks.cupidly.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.tomfricks.cupidly.R
import com.tomfricks.cupidly.billing.BillingManager
import com.tomfricks.cupidly.billing.PurchaseResult
import com.tomfricks.cupidly.data.EmojiUse
import com.tomfricks.cupidly.data.FlirtLevel
import com.tomfricks.cupidly.data.MessageStyle
import com.tomfricks.cupidly.data.MessageTone
import com.tomfricks.cupidly.data.PreferencesRepository
import com.tomfricks.cupidly.data.ReplyLength
import com.tomfricks.cupidly.data.ThemeMode
import com.tomfricks.cupidly.data.UserPreferences
import com.tomfricks.cupidly.ui.theme.PebbleButton
import com.tomfricks.cupidly.ui.theme.PebbleDialog
import com.tomfricks.cupidly.ui.theme.PebbleOption
import com.tomfricks.cupidly.ui.theme.PebbleRow
import com.tomfricks.cupidly.ui.theme.PebbleTextButton
import com.tomfricks.cupidly.ui.theme.PebbleTone
import com.tomfricks.cupidly.ui.theme.ProBadge
import kotlinx.coroutines.launch

/** Display names of the choices that only Hook Pro can pick. */
private val proTones = MessageTone.values().filter { it.proOnly }.map { it.displayName }.toSet()
private val proFlirtLevels = FlirtLevel.values().filter { it.proOnly }.map { it.displayName }.toSet()
private val proReplyLengths = ReplyLength.values().filter { it.proOnly }.map { it.displayName }.toSet()

@Composable
fun HomeScreen(
    onNavigateToGuide: () -> Unit,
    onNavigateToDemo: () -> Unit,
    onNavigateToPaywall: () -> Unit,
    onNavigateToCustomerCenter: () -> Unit,
    preferencesRepository: PreferencesRepository,
    userPreferences: UserPreferences,
    isPro: Boolean = false
) {
    var currentPreferences by remember { mutableStateOf(userPreferences) }
    val coroutineScope = rememberCoroutineScope()

    val billingAvailable by BillingManager.isAvailable.collectAsState()
    var isRestoring by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }

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

        // App Icon - same art as the launcher icon
        Image(
            painter = painterResource(
                id = R.mipmap.ic_launcher_foreground
            ),
            contentDescription = "Hook",
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hook",
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

        // Non-blocking upsell: always reachable, never in the way.
        PebbleRow(
            title = if (isPro) "Hook Pro" else "Upgrade to Pro",
            subtitle = if (isPro) {
                "Unlimited rizz. Thanks for the support."
            } else {
                "Unlimited rizz, every tone, every reply length."
            },
            onClick = onNavigateToPaywall,
            tone = if (isPro) PebbleTone.MUTED else PebbleTone.BLUE,
            trailingIcon = Icons.Default.ChevronRight
        )

        // Play expects subscribers to be able to manage or cancel from inside
        // the app; RevenueCat's Customer Center is that screen. Non-subscribers
        // get the lighter-weight restore affordance instead.
        if (billingAvailable) {
            Spacer(modifier = Modifier.height(12.dp))

            if (isPro) {
                PebbleRow(
                    title = "Manage subscription",
                    subtitle = "Change your plan, restore purchases or cancel.",
                    onClick = onNavigateToCustomerCenter,
                    trailingIcon = Icons.Default.ChevronRight
                )
            } else {
                PebbleRow(
                    title = if (isRestoring) "Restoring…" else "Restore purchases",
                    subtitle = "Already subscribed? Bring ${BillingManager.PRO_DISPLAY_NAME} back.",
                    onClick = {
                        if (!isRestoring) {
                            isRestoring = true
                            coroutineScope.launch {
                                val result = BillingManager.restorePurchases()
                                isRestoring = false
                                restoreMessage = when (result) {
                                    is PurchaseResult.Success ->
                                        "${BillingManager.PRO_DISPLAY_NAME} restored."
                                    is PurchaseResult.Cancelled -> null
                                    is PurchaseResult.Failed -> result.message
                                }
                            }
                        }
                    },
                    trailingIcon = Icons.Default.ChevronRight
                )
            }
        }

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
            },
            proOptions = proTones,
            isPro = isPro,
            onProRequired = {
                showToneDropdown = false
                onNavigateToPaywall()
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
            },
            proOptions = proFlirtLevels,
            isPro = isPro,
            onProRequired = {
                showFlirtDropdown = false
                onNavigateToPaywall()
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
            },
            proOptions = proReplyLengths,
            isPro = isPro,
            onProRequired = {
                showLengthDropdown = false
                onNavigateToPaywall()
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

    // Outcome of a "Restore purchases" tap.
    val currentRestoreMessage = restoreMessage
    if (currentRestoreMessage != null) {
        PebbleDialog(
            title = "Restore purchases",
            subtitle = currentRestoreMessage,
            onDismiss = { restoreMessage = null }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PebbleTextButton(
                    text = "OK",
                    onClick = { restoreMessage = null }
                )
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        PebbleDialog(
            title = "Reset Settings",
            subtitle = "This will reset all your preferences to default values. This action cannot be undone.",
            onDismiss = { showResetDialog = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PebbleTextButton(
                    text = "Cancel",
                    onClick = { showResetDialog = false }
                )
                PebbleTextButton(
                    text = "Reset",
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        coroutineScope.launch {
                            preferencesRepository.resetAllSettings()
                            currentPreferences = UserPreferences()
                        }
                        showResetDialog = false
                    }
                )
            }
        }
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

/**
 * @param proOptions display names that need Hook Pro. They render with a "PRO"
 *   badge and route to the paywall via [onProRequired] instead of being picked.
 */
@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    proOptions: Set<String> = emptySet(),
    isPro: Boolean = true,
    onProRequired: () -> Unit = {}
) {
    PebbleDialog(title = title, onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val locked = !isPro && option in proOptions
                val trailing: (@Composable () -> Unit)? = if (locked) {
                    { ProBadge() }
                } else {
                    null
                }
                PebbleOption(
                    text = option,
                    selected = option == currentValue,
                    onClick = { if (locked) onProRequired() else onSelect(option) },
                    trailing = trailing
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            PebbleTextButton(text = "Cancel", onClick = onDismiss)
        }
    }
}

@Composable
fun BioEditDialog(
    currentBio: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var bioText by remember { mutableStateOf(currentBio) }

    PebbleDialog(
        title = "Edit Bio",
        subtitle = "The more Hook knows, the sharper the rizz.",
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            value = bioText,
            onValueChange = { bioText = it },
            placeholder = { Text("Tell us about yourself") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            PebbleTextButton(text = "Cancel", onClick = onDismiss)
            PebbleTextButton(text = "Save", onClick = { onSave(bioText) })
        }
    }
}
