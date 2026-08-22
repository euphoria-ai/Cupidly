package com.tomfricks.hook.service

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
import com.tomfricks.hook.api.ApiService
import com.tomfricks.hook.api.GenerateRepliesResult
import com.tomfricks.hook.api.ReplyError
import com.tomfricks.hook.keyboard.ConversationSession
import com.tomfricks.hook.data.PreferencesRepository
import com.tomfricks.hook.data.UserPreferences
import com.tomfricks.hook.keyboard.RizzSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

class ScreenshotDetectionService : Service() {
    
    private var screenshotObserver: ContentObserver? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastScreenshotPath: String? = null
    private var lastScreenshotTime: Long = 0
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var apiService: ApiService
    
    companion object {
        const val ACTION_START = "com.tomfricks.hook.ACTION_START_DETECTION"
        const val ACTION_STOP = "com.tomfricks.hook.ACTION_STOP_DETECTION"
        
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "screenshot_detection"
        
        var onScreenshotDetected: ((Bitmap) -> Unit)? = null
        var onAutoReplyGenerated: ((List<String>) -> Unit)? = null
        var onAutoReplyFailed: ((String) -> Unit)? = null
        var isServiceRunning = false
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        preferencesRepository = PreferencesRepository(this)
        // Needs a Context to resolve this install's id for the auth headers.
        apiService = ApiService(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Play policy requires an ongoing, user-visible notification for the
        // whole life of a FOREGROUND_SERVICE_DATA_SYNC service. Promote to the
        // foreground on *every* start before doing anything else — including the
        // null-intent redelivery the OS sends when it restarts a START_STICKY
        // service, and the ACTION_STOP path. This guarantees the notification is
        // never missing and never breaks the startForegroundService() contract
        // (which mandates a startForeground call within ~5s of the launch).
        startForegroundWithNotification()

        when (intent?.action) {
            ACTION_STOP -> {
                stopScreenshotDetection()
                isServiceRunning = false
                stopSelf()
            }
            // ACTION_START, or a null-intent restart of the sticky service:
            // (re)start detection and keep the notification up.
            else -> {
                startScreenshotDetection()
                isServiceRunning = true
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        // For Android 14+, the service type must be passed explicitly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopScreenshotDetection()
        isServiceRunning = false
        serviceScope.cancel()
    }
    
    private fun startScreenshotDetection() {
        // Already watching — a sticky restart must not stack a second observer.
        if (screenshotObserver != null) return

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
                            
                            // MediaStore announces the file while it is still being
                            // written (".pending-…"); reading it then throws EACCES.
                            if (File(path).name.startsWith(".pending-")) {
                                return@use
                            }

                            // Check if it's a screenshot (contains "screenshot" in path)
                            if (path.lowercase().contains("screenshot") ||
                                path.lowercase().contains("screen shot") ||
                                path.lowercase().contains("screen_shot")) {
                                
                                // Avoid processing same screenshot twice
                                // Window is generous: MediaStore can announce the
                                // finalized file several seconds after capture.
                                if (path != lastScreenshotPath &&
                                    System.currentTimeMillis() - dateAdded < 20000) {
                                    
                                    lastScreenshotPath = path
                                    lastScreenshotTime = System.currentTimeMillis()
                                    
                                    // Load and process screenshot
                                    val bitmap = loadScreenshot(path)
                                    if (bitmap != null) {
                                        // Record the round centrally first, so it
                                        // survives with or without a live keyboard.
                                        // One screenshot buys exactly one round:
                                        // a null claim means the keyboard got
                                        // there first (it claims on show, for
                                        // screenshots taken while the app was
                                        // cold), and the same image is never
                                        // generated from twice.
                                        val generation = withContext(Dispatchers.Main) {
                                            RizzSession.onScreenshot(bitmap)
                                            onScreenshotDetected?.invoke(bitmap)
                                            RizzSession.claimForScreenshot()
                                        }
                                        if (generation != null) {
                                            generateAutoReplies(generation)
                                        }

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
                "Hook Screenshot Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Detects screenshots for AI reply generation"
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Run the round [generation] was granted for. Its token is what keeps a
     * slow answer from overwriting a newer one: if the user screenshots again
     * mid-flight, this result is dropped and the newer round owns the UI.
     */
    private fun generateAutoReplies(generation: RizzSession.Generation) {
        serviceScope.launch(Dispatchers.IO) {
            val token = generation.token

            // Carry the hidden session context into the request; it keeps
            // building even across screenshots where no reply was picked.
            val snapshot = ConversationSession.snapshot()
            val prefs = try {
                preferencesRepository.userPreferencesFlow.first()
            } catch (e: Exception) {
                Log.e("ScreenshotDetection", "Could not read preferences", e)
                withContext(Dispatchers.Main) { fail(token, ReplyError.SERVER) }
                return@launch
            }

            when (
                val result =
                    apiService.generateReplies(generation.screenshot, prefs, snapshot.context)
            ) {
                is GenerateRepliesResult.Success -> {
                    val generated = result.replies
                    withContext(Dispatchers.Main) {
                        // A superseded round leaves nothing behind — not the
                        // suggestions, and not the hidden context either. Every
                        // mutation of the session happens on this thread, so the
                        // check and the writes can't interleave with a new claim.
                        if (!RizzSession.isLive(token)) {
                            Log.d("ScreenshotDetection", "Dropping superseded round $token")
                            return@withContext
                        }
                        // Persist the updated context (in memory only) before the
                        // UI shows the fresh suggestions.
                        ConversationSession.update(snapshot.sessionId, generated.context)
                        RizzSession.onSuggestions(token, generated.suggestions)
                        onAutoReplyGenerated?.invoke(generated.suggestions)
                        Log.d(
                            "ScreenshotDetection",
                            "Auto-generated ${generated.suggestions.size} replies"
                        )
                    }
                }

                is GenerateRepliesResult.AllowanceExhausted -> {
                    // The keyboard turns this into an upgrade prompt rather
                    // than an error message.
                    Log.i(
                        "ScreenshotDetection",
                        "Free allowance spent (${result.used}/${result.freeLimit})"
                    )
                    withContext(Dispatchers.Main) {
                        RizzSession.onAllowanceExhausted(token)
                        onAutoReplyFailed?.invoke("You're out of free rizz. Go Pro for unlimited.")
                    }
                }

                is GenerateRepliesResult.Failure -> {
                    Log.e("ScreenshotDetection", "Generation failed: ${result.error}")
                    withContext(Dispatchers.Main) { fail(token, result.error) }
                }
            }
        }
    }

    private fun fail(token: Long, error: ReplyError) {
        RizzSession.onFailure(token, error)
        onAutoReplyFailed?.invoke(error.userMessage)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Hook is active")
            .setContentText("Take a screenshot for automatic AI replies")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }
}

