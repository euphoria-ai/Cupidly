package com.tomlin7.l0v3.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.FileObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tomlin7.l0v3.api.GeminiService
import com.tomlin7.l0v3.data.PreferencesRepository
import com.tomlin7.l0v3.data.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

class ScreenshotDetectionService : Service() {
    
    private var screenshotObserver: ContentObserver? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastScreenshotPath: String? = null
    private var lastScreenshotTime: Long = 0
    private lateinit var preferencesRepository: PreferencesRepository
    
    companion object {
        const val ACTION_START = "com.tomlin7.l0v3.ACTION_START_DETECTION"
        const val ACTION_STOP = "com.tomlin7.l0v3.ACTION_STOP_DETECTION"
        
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "screenshot_detection"
        
        var onScreenshotDetected: ((Bitmap) -> Unit)? = null
        var onAutoReplyGenerated: ((List<String>) -> Unit)? = null
        var isServiceRunning = false
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        preferencesRepository = PreferencesRepository(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                // For Android 14+, specify service type when starting foreground
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, createNotification())
                }
                startScreenshotDetection()
                isServiceRunning = true
            }
            ACTION_STOP -> {
                stopScreenshotDetection()
                isServiceRunning = false
                stopSelf()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopScreenshotDetection()
        isServiceRunning = false
        serviceScope.cancel()
    }
    
    private fun startScreenshotDetection() {
        // Monitor external storage for new screenshots
        val handler = Handler(Looper.getMainLooper())
        
        screenshotObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                uri?.let { detectScreenshot(it) }
            }
        }
        
        // Monitor Screenshots directory
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            screenshotObserver!!
        )
        
        Log.d("ScreenshotDetection", "Screenshot detection started")
    }
    
    private fun detectScreenshot(uri: Uri) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val projection = arrayOf(
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_ADDED
                )
                
                contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val pathIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        val dateIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                        
                        if (pathIndex >= 0 && dateIndex >= 0) {
                            val path = cursor.getString(pathIndex)
                            val dateAdded = cursor.getLong(dateIndex) * 1000
                            
                            // Check if it's a screenshot (contains "screenshot" in path)
                            if (path.lowercase().contains("screenshot") || 
                                path.lowercase().contains("screen shot") ||
                                path.lowercase().contains("screen_shot")) {
                                
                                // Avoid processing same screenshot twice
                                if (path != lastScreenshotPath && 
                                    System.currentTimeMillis() - dateAdded < 5000) {
                                    
                                    lastScreenshotPath = path
                                    lastScreenshotTime = System.currentTimeMillis()
                                    
                                    // Load and process screenshot
                                    val bitmap = loadScreenshot(path)
                                    if (bitmap != null) {
                                        withContext(Dispatchers.Main) {
                                            onScreenshotDetected?.invoke(bitmap)
                                        }
                                        
                                        // Automatically generate replies
                                        generateAutoReplies(bitmap)
                                        
                                        Log.d("ScreenshotDetection", "Screenshot detected and processed: $path")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ScreenshotDetection", "Error detecting screenshot", e)
            }
        }
    }
    
    private fun loadScreenshot(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) {
                // Load bitmap with size optimization
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(path, options)
                
                // Calculate sample size to reduce memory usage
                options.inSampleSize = calculateInSampleSize(options, 1080, 1920)
                options.inJustDecodeBounds = false
                
                BitmapFactory.decodeFile(path, options)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ScreenshotDetection", "Error loading screenshot", e)
            null
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun stopScreenshotDetection() {
        screenshotObserver?.let {
            contentResolver.unregisterContentObserver(it)
        }
        screenshotObserver = null
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "L0V3 Screenshot Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Detects screenshots for AI reply generation"
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    private fun generateAutoReplies(bitmap: Bitmap) {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val prefs = preferencesRepository.userPreferencesFlow.first()
                
                if (prefs.geminiApiKey.isEmpty()) {
                    Log.w("ScreenshotDetection", "No API key available for auto-reply generation")
                    return@launch
                }
                
                val geminiService = GeminiService(prefs.geminiApiKey)
                val result = geminiService.generateReplies(bitmap, prefs)
                
                result.onSuccess { replies ->
                    withContext(Dispatchers.Main) {
                        onAutoReplyGenerated?.invoke(replies)
                    }
                    Log.d("ScreenshotDetection", "Auto-generated ${replies.size} replies")
                }.onFailure { error ->
                    Log.e("ScreenshotDetection", "Error generating auto replies", error)
                }
            } catch (e: Exception) {
                Log.e("ScreenshotDetection", "Error generating auto replies", e)
            }
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("L0V3 is active")
            .setContentText("Take a screenshot for automatic AI replies")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}

