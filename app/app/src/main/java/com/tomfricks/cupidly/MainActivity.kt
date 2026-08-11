package com.tomfricks.cupidly

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tomfricks.cupidly.data.UserPreferences
import com.tomfricks.cupidly.ui.navigation.CupidlyNavigation
import com.tomfricks.cupidly.ui.theme.CupidlyTheme

class MainActivity : ComponentActivity() {
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission handled, nothing special needed
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request storage permission for screenshot detection
        requestStoragePermissionIfNeeded()
        
        setContent {
            val app = application as CupidlyApplication
            val userPreferences by app.preferencesRepository.userPreferencesFlow.collectAsState(
                initial = UserPreferences()
            )
            
            CupidlyTheme(themeMode = userPreferences.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CupidlyNavigation()
                }
            }
        }
    }
    
    private fun requestStoragePermissionIfNeeded() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(permission)
        }
    }
}

