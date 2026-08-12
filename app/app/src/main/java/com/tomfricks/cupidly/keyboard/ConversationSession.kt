package com.tomfricks.cupidly.keyboard

import com.tomfricks.cupidly.api.ConversationContext
import java.util.UUID

/**
 * In-memory, session-only holder for the hidden conversation context.
 *
 * It accumulates understanding of the whole dating-app conversation across
 * screenshots and picked replies. It is NEVER shown in any UI surface and NEVER
 * persisted to DataStore/disk — it only lives for as long as the process does.
 * It survives the keyboard being hidden/shown within a session, and is wiped
 * when the user starts a new session.
 */
object ConversationSession {

    @Volatile
    var conversationContext: ConversationContext = ConversationContext()
        private set

    @Volatile
    var sessionId: String = UUID.randomUUID().toString()
        private set

    /** Replace the held context with the server's updated one (summary + passthrough). */
    @Synchronized
    fun update(context: ConversationContext) {
        conversationContext = context
    }

    /** Record a reply the user actually picked & sent. */
    @Synchronized
    fun recordSentReply(reply: String) {
        conversationContext = conversationContext.copy(
            sentReplies = conversationContext.sentReplies + reply
        )
    }

    /** Start a fresh session: wipe the hidden context and mint a new session id. */
    @Synchronized
    fun reset() {
        conversationContext = ConversationContext()
        sessionId = UUID.randomUUID().toString()
    }
}
