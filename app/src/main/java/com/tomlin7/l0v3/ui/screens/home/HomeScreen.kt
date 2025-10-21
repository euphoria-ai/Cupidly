package com.tomlin7.l0v3.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.tomlin7.l0v3.R
import com.tomlin7.l0v3.data.*

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
    var showGenderDropdown by remember { mutableStateOf(false) }
    var showPronounsDropdown by remember { mutableStateOf(false) }
    var showBioDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(userPreferences) {
        currentPreferences = userPreferences
    }
    
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
        
        // App Icon - using your custom PNG
        Image(
            painter = painterResource(id = R.drawable.heart),
            contentDescription = "L0V3",
            modifier = Modifier.size(100.dp),
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
        
        // Getting Started Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToGuide() },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFD946A6)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Getting Started",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Learn how to use L0V3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Clean Settings List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Message Settings Section
            Text(
                text = "Message Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            
            // Style Setting Card
            SettingCard(
                icon = Icons.Default.TextFields,
                title = "Style",
                subtitle = currentPreferences.style.displayName,
                onClick = { showStyleDropdown = true }
            )
            
            // Tone Setting Card
            SettingCard(
                icon = Icons.Default.Chat,
                title = "Tone",
                subtitle = currentPreferences.tone.displayName,
                onClick = { showToneDropdown = true }
            )
            
            // Flirt Level Setting Card
            SettingCard(
                icon = Icons.Default.LocalFireDepartment,
                title = "Flirt Level",
                subtitle = currentPreferences.flirtLevel.displayName,
                onClick = { showFlirtDropdown = true }
            )
            
            // Reply Length Setting Card
            SettingCard(
                icon = Icons.Default.FormatSize,
                title = "Reply Length",
                subtitle = currentPreferences.replyLength.displayName,
                onClick = { showLengthDropdown = true }
            )
            
            // Emoji Use Setting Card
            SettingCard(
                icon = Icons.Default.EmojiEmotions,
                title = "Emoji Use",
                subtitle = currentPreferences.emojiUse.displayName,
                onClick = { showEmojiDropdown = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Profile Info Section
            Text(
                text = "Profile Info",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            
            // Gender Setting Card
            SettingCard(
                icon = Icons.Default.Person,
                title = "Gender",
                subtitle = currentPreferences.profileGender.ifEmpty { "Select gender" },
                onClick = { showGenderDropdown = true }
            )
            
            // Pronouns Setting Card
            SettingCard(
                icon = Icons.Default.People,
                title = "Pronouns",
                subtitle = currentPreferences.profilePronouns.ifEmpty { "Select pronouns" },
                onClick = { showPronounsDropdown = true }
            )
            
            // Bio Setting Card
            SettingCard(
                icon = Icons.Default.Description,
                title = "Bio",
                subtitle = currentPreferences.profileBio.ifEmpty { "Tell us about yourself" },
                onClick = { showBioDialog = true }
            )
        }
        
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
    
    if (showGenderDropdown) {
        SelectionDialog(
            title = "Select Gender",
            options = listOf("Male", "Female", "Non-binary", "Other", "Prefer not to say"),
            currentValue = currentPreferences.profileGender.ifEmpty { "Select gender" },
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
            options = listOf("He/Him", "She/Her", "They/Them", "Other", "Prefer not to say"),
            currentValue = currentPreferences.profilePronouns.ifEmpty { "Select pronouns" },
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
}

@Composable
fun SettingCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF333333),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Title and Subtitle
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
            
            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color(0xFF999999),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdownMenu(
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    val genders = listOf("Male", "Female", "Non-binary", "Other", "Prefer not to say")
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentValue.ifEmpty { "Select gender" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Gender") },
            trailingIcon = { 
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD946A6),
                focusedLabelColor = Color(0xFFD946A6)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            genders.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(gender) },
                    onClick = {
                        onValueChange(gender)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PronounsDropdownMenu(
    currentValue: String,
    onValueChange: (String) -> Unit
) {
    val pronouns = listOf("He/Him", "She/Her", "They/Them", "Other", "Prefer not to say")
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentValue.ifEmpty { "Select pronouns" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Pronouns") },
            trailingIcon = { 
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFD946A6),
                focusedLabelColor = Color(0xFFD946A6)
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            pronouns.forEach { pronoun ->
                DropdownMenuItem(
                    text = { Text(pronoun) },
                    onClick = {
                        onValueChange(pronoun)
                        expanded = false
                    }
                )
            }
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
                color = Color(0xFFD946A6)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(options) { option ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(option) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (option == currentValue) 
                                Color(0xFFD946A6).copy(alpha = 0.1f) 
                            else Color.Transparent
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = option,
                            modifier = Modifier.padding(16.dp),
                            color = if (option == currentValue) 
                                Color(0xFFD946A6) 
                            else Color(0xFF333333),
                            fontWeight = if (option == currentValue) 
                                FontWeight.Bold 
                            else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFD946A6))
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
                color = Color(0xFFD946A6)
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
                    focusedBorderColor = Color(0xFFD946A6),
                    focusedLabelColor = Color(0xFFD946A6)
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(bioText) }) {
                Text("Save", color = Color(0xFFD946A6))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFD946A6))
            }
        }
    )
}
