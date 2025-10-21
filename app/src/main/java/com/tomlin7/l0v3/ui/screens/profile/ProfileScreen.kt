package com.tomlin7.l0v3.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tomlin7.l0v3.data.PreferencesRepository
import com.tomlin7.l0v3.data.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    preferencesRepository: PreferencesRepository,
    userPreferences: UserPreferences
) {
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(userPreferences.profileName) }
    var gender by remember { mutableStateOf(userPreferences.profileGender) }
    var sexuality by remember { mutableStateOf(userPreferences.profileSexuality) }
    var bio by remember { mutableStateOf(userPreferences.profileBio) }
    var pronouns by remember { mutableStateOf(userPreferences.profilePronouns) }
    
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
            title = { Text("Profile") },
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
            Text(
                text = "Personalize Your AI",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD946A6)
            )
            
            Text(
                text = "Help L0V3 understand you better for more personalized replies",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD946A6),
                            focusedLabelColor = Color(0xFFD946A6)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Gender (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD946A6),
                            focusedLabelColor = Color(0xFFD946A6)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = pronouns,
                        onValueChange = { pronouns = it },
                        label = { Text("Pronouns (optional)") },
                        placeholder = { Text("e.g., he/him, she/her, they/them") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD946A6),
                            focusedLabelColor = Color(0xFFD946A6)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = sexuality,
                        onValueChange = { sexuality = it },
                        label = { Text("Sexuality (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD946A6),
                            focusedLabelColor = Color(0xFFD946A6)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text("Bio (optional)") },
                        placeholder = { Text("Tell us a bit about yourself...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD946A6),
                            focusedLabelColor = Color(0xFFD946A6)
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    scope.launch {
                        preferencesRepository.updateProfile(
                            name = name,
                            gender = gender,
                            sexuality = sexuality,
                            bio = bio,
                            pronouns = pronouns
                        )
                        onNavigateBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD946A6)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Profile", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
