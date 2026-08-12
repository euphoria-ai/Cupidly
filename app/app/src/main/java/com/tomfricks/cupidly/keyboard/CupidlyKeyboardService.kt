package com.tomfricks.cupidly.keyboard

import android.content.Intent
import android.graphics.Bitmap
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
                    // Rendered straight off the shared session, so the whole
                    // transcript — every screenshot, reply and typing bubble —
                    // that built up while this keyboard did not exist is already
                    // here.
                    KeyboardPanel(
                        state = RizzSession.status,
                        items = RizzSession.items,
                        errorMessage = RizzSession.error,
                        isDarkTheme = isDarkTheme,
                        onGenerate = ::onGenerateClicked,
                        onSuggestionClick = ::onSuggestionClicked,
                        onNewChatClick = ::onNewChatClicked,
                        onSettingsClick = ::openSettings,
                        onBackspaceClick = ::onBackspaceClicked
                    )
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        // Typical flow: screenshot the chat, *then* open the keyboard. Anything
        // generated in the meantime is kept; only a stale round is dropped.
        RizzSession.clearIfStale()

        // Opened onto a screenshot nobody has processed yet (detection fired
        // while the app was cold, or generation died with no keyboard alive):
        // start it here rather than waiting for a tap.
        if (RizzSession.status != RizzSession.Status.GENERATING &&
            !RizzSession.hasPendingSuggestions &&
            RizzSession.hasFreshScreenshot
        ) {
            RizzSession.latestScreenshot?.let { generateReplies(it) }
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
        // Ignore taps while a round is already in flight — the button is also
        // disabled in the UI, this just guards the callback.
        if (RizzSession.status == RizzSession.Status.GENERATING) return

        val screenshot = RizzSession.latestScreenshot
        if (screenshot != null && RizzSession.hasFreshScreenshot) {
            // "Generate more rizz": another round on the latest screenshot.
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

            // Carry the hidden session context into the request and store the
            // server's updated context (in memory only) before showing replies.
            val currentContext = ConversationSession.conversationContext
            val result = apiService?.generateReplies(screenshot, prefs, currentContext)

            result?.onSuccess { generated ->
                ConversationSession.update(generated.context)
                RizzSession.onSuggestions(generated.suggestions)
                Log.d("CupidlyKeyboard", "Generated ${generated.suggestions.size} suggestions")
            }?.onFailure { error ->
                RizzSession.onFailure(error.message ?: "Couldn't reach Hook")
                Log.e("CupidlyKeyboard", "Error generating replies", error)
            }
        }
    }

    private fun onSuggestionClicked(suggestion: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(suggestion, 1)

        // Keep the panel open: the tapped reply moves into the sent transcript
        // and is recorded in the hidden session context so the next generation
        // knows what was actually sent.
        RizzSession.markSent(suggestion)
        ConversationSession.recordSentReply(suggestion)
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /**
     * Start a fresh session: wipe the hidden conversation context and every
     * on-screen trace so the next screenshot begins a brand-new conversation.
     */
    private fun onNewChatClicked() {
        ConversationSession.reset()
        RizzSession.reset()
        Log.d("CupidlyKeyboard", "New session started")
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
