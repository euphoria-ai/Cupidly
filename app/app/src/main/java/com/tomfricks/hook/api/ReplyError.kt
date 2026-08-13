package com.tomfricks.hook.api

/**
 * Every way a generation can fail, as a closed set of *product* states.
 *
 * The keyboard only ever renders [userMessage]. Server text, exception
 * messages and HTTP codes never reach a UI surface — they go to Logcat and
 * stop there — so a Groq rate limit reads as "Hook is busy", not as
 * "POST /generate-replies failed with HTTP 429".
 */
enum class ReplyError {
    /** No usable network, or the server could not be reached at all. */
    NO_CONNECTION,

    /** The request went out but nothing came back in time. */
    TIMEOUT,

    /** Upstream is rate limited (HTTP 429) — a wait fixes it, a retry doesn't. */
    BUSY,

    /** Anything else the server got wrong (5xx, malformed body, empty reply). */
    SERVER,

    /** This install isn't identified to the server yet (HTTP 400 / 401). */
    SETUP,

    /** The model couldn't make anything of this image. */
    UNREADABLE,

    /** Nothing to generate from: no screenshot, or one too old to still be right. */
    NO_SCREENSHOT;

    /** The only string any surface is allowed to show for a failure. */
    val userMessage: String
        get() = when (this) {
            NO_CONNECTION -> "No connection. Check your internet and try again."
            TIMEOUT -> "That took a bit too long. Give it another go."
            BUSY -> "Hook is catching its breath. Try again in a few seconds."
            SERVER -> "Hook couldn't write replies just now. Try again in a moment."
            SETUP -> "Open the Hook app once to finish setup, then try again."
            UNREADABLE -> "Couldn't read that screenshot. Try a clearer one."
            NO_SCREENSHOT -> "Take a screenshot (Power + Volume Down) — replies appear on their own."
        }
}
