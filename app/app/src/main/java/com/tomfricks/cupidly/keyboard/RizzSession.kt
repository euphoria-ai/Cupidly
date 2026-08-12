package com.tomfricks.cupidly.keyboard

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Process-wide state for one *session* of "screenshot -> replies".
 *
 * It deliberately does *not* live on [CupidlyKeyboardService]: the IME is
 * created and destroyed constantly, and the screenshot usually arrives while
 * no input view exists at all. Keeping the session here means detection can run,
 * finish, and store replies with no keyboard on screen — whichever IME
 * instance shows up next simply renders what is already here.
 *
 * A session is an ordered transcript ([items]) that keeps growing: each new
 * screenshot is appended as a follow-up image, replies land under it, and a
 * picked reply becomes a sent bubble. Only the "+" button ([reset]) clears it.
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

    /** One entry in the ongoing chat transcript, rendered top-to-bottom. */
    sealed interface TranscriptItem {
        /** A captured screenshot, shown as an attachment card. */
        data class Screenshot(val bitmap: Bitmap) : TranscriptItem

        /** A reply the user picked and committed — no longer tappable. */
        data class SentReply(val text: String) : TranscriptItem

        /** A pending suggestion the user can still tap to send. */
        data class Suggestion(val text: String) : TranscriptItem

        /** The animated "typing" bubble shown while replies are generating. */
        data object Typing : TranscriptItem
    }

    /** A screenshot older than this is no longer worth generating from. */
    const val SCREENSHOT_TTL_MS = 2 * 60 * 1000L

    /** How long a finished session stays on screen. */
    private const val SUGGESTIONS_TTL_MS = 5 * 60 * 1000L

    var status by mutableStateOf(Status.IDLE)
        private set

    /** The ordered transcript: screenshots, sent replies, suggestions, typing. */
    var items by mutableStateOf<List<TranscriptItem>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var screenshotAt: Long = 0L
        private set

    private var suggestionsAt: Long = 0L

    /** The most recent screenshot in the session, used for "generate more". */
    val latestScreenshot: Bitmap?
        get() = items.filterIsInstance<TranscriptItem.Screenshot>().lastOrNull()?.bitmap

    /** True while the newest screenshot is recent enough to generate replies from. */
    val hasFreshScreenshot: Boolean
        get() = latestScreenshot != null &&
            System.currentTimeMillis() - screenshotAt < SCREENSHOT_TTL_MS

    /** True while any tappable suggestion is still pending. */
    val hasPendingSuggestions: Boolean
        get() = items.any { it is TranscriptItem.Suggestion }

    /** True when the session still holds fresh, worth-showing replies. */
    val hasFreshSuggestions: Boolean
        get() = hasPendingSuggestions &&
            System.currentTimeMillis() - suggestionsAt < SUGGESTIONS_TTL_MS

    /** True once the session has any reply activity (sent or pending). */
    val hasReplies: Boolean
        get() = items.any { it is TranscriptItem.SentReply || it is TranscriptItem.Suggestion }

    /**
     * A new screenshot continues the session instead of resetting it: any
     * un-picked suggestions from the previous screenshot are discarded, the new
     * screenshot is appended as a follow-up image, and generation starts.
     */
    fun onScreenshot(bitmap: Bitmap) {
        screenshotAt = System.currentTimeMillis()
        error = null
        items = items.filterNot { it is TranscriptItem.Suggestion || it is TranscriptItem.Typing } +
            TranscriptItem.Screenshot(bitmap) +
            TranscriptItem.Typing
        status = Status.GENERATING
    }

    /** Start (or restart) generation on the current screenshot without adding one. */
    fun onGenerating() {
        error = null
        items = items.filterNot { it is TranscriptItem.Suggestion || it is TranscriptItem.Typing } +
            TranscriptItem.Typing
        status = Status.GENERATING
    }

    fun onSuggestions(replies: List<String>) {
        suggestionsAt = System.currentTimeMillis()
        error = null
        items = items.filterNot { it is TranscriptItem.Typing } +
            replies.map { TranscriptItem.Suggestion(it) }
        status = Status.SHOWING
    }

    fun onFailure(reason: String) {
        error = reason
        items = items.filterNot { it is TranscriptItem.Typing }
        status = Status.ERROR
    }

    /**
     * Keep a tapped reply and drop the rest: the chosen suggestion becomes a
     * sent bubble at the bottom of the transcript and every other pending
     * suggestion is removed.
     */
    fun markSent(reply: String) {
        items = items.filterNot { it is TranscriptItem.Suggestion } +
            TranscriptItem.SentReply(reply)
        suggestionsAt = System.currentTimeMillis()
        status = Status.SHOWING
    }

    /** Drop a session that is too old to be about the conversation on screen. */
    fun clearIfStale() {
        if (status != Status.GENERATING && !hasFreshSuggestions && !hasFreshScreenshot) {
            reset()
        }
    }

    fun reset() {
        status = Status.IDLE
        items = emptyList()
        error = null
        screenshotAt = 0L
        suggestionsAt = 0L
    }
}
