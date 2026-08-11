package com.tomfricks.cupidly.keyboard

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-wide state for one "screenshot -> replies" round.
 *
 * It deliberately does *not* live on [CupidlyKeyboardService]: the IME is
 * created and destroyed constantly, and the screenshot usually arrives while
 * no input view exists at all. Keeping the round here means detection can run,
 * finish, and store replies with no keyboard on screen — whichever IME
 * instance shows up next simply renders what is already here.
 */
object RizzSession {

    enum class Status {
        /** Nothing captured yet. */
        IDLE,

        /** Screenshot in hand, replies being written. */
        GENERATING,

        /** Replies ready (or partly sent). */
        SHOWING,

        /** Generation failed. */
        ERROR
    }

    /** A screenshot older than this is no longer worth generating from. */
    const val SCREENSHOT_TTL_MS = 2 * 60 * 1000L

    /** How long finished replies stay on screen. */
    private const val SUGGESTIONS_TTL_MS = 5 * 60 * 1000L

    var status by mutableStateOf(Status.IDLE)
        private set

    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set

    var sent by mutableStateOf<List<String>>(emptyList())
        private set

    var screenshot by mutableStateOf<Bitmap?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var screenshotAt: Long = 0L
        private set

    private var suggestionsAt: Long = 0L

    /** True while a screenshot is recent enough to generate replies from. */
    val hasFreshScreenshot: Boolean
        get() = screenshot != null &&
            System.currentTimeMillis() - screenshotAt < SCREENSHOT_TTL_MS

    /** True when finished replies are still worth showing. */
    val hasFreshSuggestions: Boolean
        get() = suggestions.isNotEmpty() &&
            System.currentTimeMillis() - suggestionsAt < SUGGESTIONS_TTL_MS

    /** A new screenshot starts a new round and clears the previous one. */
    fun onScreenshot(bitmap: Bitmap) {
        screenshot = bitmap
        screenshotAt = System.currentTimeMillis()
        suggestions = emptyList()
        sent = emptyList()
        error = null
        status = Status.GENERATING
    }

    fun onGenerating() {
        error = null
        status = Status.GENERATING
    }

    fun onSuggestions(replies: List<String>) {
        suggestions = replies
        suggestionsAt = System.currentTimeMillis()
        error = null
        status = Status.SHOWING
    }

    fun onFailure(reason: String) {
        error = reason
        status = Status.ERROR
    }

    /** Move a tapped reply out of the suggestions and into the sent list. */
    fun markSent(reply: String) {
        sent = sent + reply
        suggestions = suggestions.filterNot { it == reply }
        suggestionsAt = System.currentTimeMillis()
    }

    /** Drop a round that is too old to be about the conversation on screen. */
    fun clearIfStale() {
        if (status != Status.GENERATING && !hasFreshSuggestions && !hasFreshScreenshot) {
            reset()
        }
    }

    fun reset() {
        status = Status.IDLE
        suggestions = emptyList()
        sent = emptyList()
        error = null
        screenshot = null
        screenshotAt = 0L
        suggestionsAt = 0L
    }
}
