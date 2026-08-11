package com.tomfricks.cupidly.keyboard

import android.content.Intent
import android.graphics.Bitmap
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.tomfricks.cupidly.MainActivity
import com.tomfricks.cupidly.api.ApiService
import com.tomfricks.cupidly.data.PreferencesRepository
import com.tomfricks.cupidly.data.ThemeMode
import com.tomfricks.cupidly.data.UserPreferences
import com.tomfricks.cupidly.service.ScreenshotDetectionService
import com.tomfricks.cupidly.ui.keyboard.KeyboardMessage
import com.tomfricks.cupidly.ui.keyboard.KeyboardPanel
import com.tomfricks.cupidly.ui.theme.CupidlyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CupidlyKeyboardService : InputMethodService(), LifecycleOwner {

    private lateinit var preferencesRepository: PreferencesRepository
    private var apiService: ApiService? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override val lifecycle: Lifecycle
        get() = lifecycleOwner.lifecycle

    private val lifecycleOwner = KeyboardLifecycleOwner()

    override fun onCreate() {
        super.onCreate()
        preferencesRepository = PreferencesRepository(this)

        // Initialize API service
        apiService = ApiService()

        // Start screenshot detection service
        startScreenshotDetectionService()

        lifecycleOwner.onCreate()
    }

    override fun onCreateInputView(): View {
        lifecycleOwner.onResume()

        // Set lifecycle owner on the InputMethodService window's DecorView
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(lifecycleOwner)
            decorView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        }

        return ComposeView(this).apply {
            // Use DisposeOnDetachedFromWindowOrReleasedFromPool strategy for IME
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
            )

            setContent {
                val userPreferences by preferencesRepository.userPreferencesFlow.collectAsState(
                    initial = UserPreferences()
                )

                val systemInDarkTheme = isSystemInDarkTheme()
                val isDarkTheme = when (userPreferences.themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> systemInDarkTheme
                }

                CupidlyTheme(themeMode = userPreferences.themeMode) {
                    // Rendered straight off the shared session, so replies that
                    // arrived while this keyboard did not exist are already here.
                    val screenshot = RizzSession.screenshot

                    KeyboardPanel(
                        state = RizzSession.status,
                        messages = buildTranscript(),
                        errorMessage = RizzSession.error,
                        isDarkTheme = isDarkTheme,
                        onGenerate = ::onGenerateClicked,
                        onSuggestionClick = ::onSuggestionClicked,
                        onPlusClick = ::openSettings,
                        onBackspaceClick = ::onBackspaceClicked,
                        thumbnail = if (screenshot != null) {
                            {
                                Image(
                                    bitmap = screenshot.asImageBitmap(),
                                    contentDescription = "Captured screenshot",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    /**
     * Sent replies stay in the transcript above the fresh suggestions, so the
     * conversation reads as a chat you can keep following up in.
     */
    private fun buildTranscript(): List<KeyboardMessage> =
        RizzSession.sent.map { KeyboardMessage(text = it, outgoing = true, sent = true) } +
            RizzSession.suggestions.map { KeyboardMessage(text = it, outgoing = true, sent = false) }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        // Typical flow: screenshot the chat, *then* open the keyboard. Anything
        // generated in the meantime is kept; only a stale round is dropped.
        RizzSession.clearIfStale()

        // Opened onto a screenshot nobody has processed yet (detection fired
        // while the app was cold, or generation died with no keyboard alive):
        // start it here rather than waiting for a tap.
        if (RizzSession.status != RizzSession.Status.GENERATING &&
            RizzSession.suggestions.isEmpty() &&
            RizzSession.hasFreshScreenshot
        ) {
            RizzSession.screenshot?.let { generateReplies(it) }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        // Don't pause here as the view might be reused
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.onDestroy()
        stopScreenshotService()
    }

    private fun onGenerateClicked() {
        val screenshot = RizzSession.screenshot
        if (screenshot != null && RizzSession.hasFreshScreenshot) {
            // "Generate more rizz": another round on the same screenshot.
            generateReplies(screenshot)
        } else {
            RizzSession.onFailure(
                "Take a screenshot (Power + Volume Down) — replies appear on their own."
            )
        }
    }

    private fun startScreenshotDetectionService() {
        val intent = Intent(this, ScreenshotDetectionService::class.java).apply {
            action = ScreenshotDetectionService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun generateReplies(screenshot: Bitmap) {
        serviceScope.launch {
            RizzSession.onGenerating()

            val prefs = preferencesRepository.userPreferencesFlow.first()
            val result = apiService?.generateReplies(screenshot, prefs)

            result?.onSuccess { suggestions ->
                RizzSession.onSuggestions(suggestions)
                Log.d("CupidlyKeyboard", "Generated ${suggestions.size} suggestions")
            }?.onFailure { error ->
                RizzSession.onFailure(error.message ?: "Couldn't reach Cupidly")
                Log.e("CupidlyKeyboard", "Error generating replies", error)
            }
        }
    }

    private fun onSuggestionClicked(suggestion: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(suggestion, 1)

        // Keep the panel open: the tapped reply moves into the sent transcript
        // so follow-ups can continue from it.
        RizzSession.markSent(suggestion)
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun onBackspaceClicked() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun stopScreenshotService() {
        val intent = Intent(this, ScreenshotDetectionService::class.java).apply {
            action = ScreenshotDetectionService.ACTION_STOP
        }
        startService(intent)
    }
}
