package com.tomfricks.hook.keyboard

import com.tomfricks.hook.api.ConversationContext
import java.util.UUID

/**
 * In-memory, session-only holder for the hidden conversation context.
 *
 * It accumulates understanding of the whole dating-app conversation across
 * screenshots and picked replies. It is NEVER shown in any UI surface and NEVER
 * persisted to DataStore/disk — it only lives for as long as the process does.
 * It survives the keyboard being hidden/shown within a session, and is wiped
 * when the user starts a new session.
 *
 * What goes in:
 *  - the rolling summary the server rewrites on every round, folding in the
 *    newest screenshot without losing what came before;
 *  - the replies the user actually picked and sent.
 *
 * What never goes in: suggestions the user ignored. If three replies are
 * generated and none is tapped, the next screenshot supersedes them and they
 * leave no trace — which is the point, since the user rejected them.
 */
object ConversationSession {

    /**
     * The context as it stood when a request went out, tagged with the session
     * it belongs to. Hand [sessionId] back to [update] so an answer from a chat
     * the user has since closed is discarded.
     */
    data class Snapshot(val sessionId: String, val context: ConversationContext)

    /** Cap on remembered sent replies, so a long session can't grow the prompt without bound. */
    private const val MAX_SENT_REPLIES = 20

    @Volatile
    var conversationContext: ConversationContext = ConversationContext()
        private set

    @Volatile
    var sessionId: String = UUID.randomUUID().toString()
        private set

    /** What to send with the next request. */
    @Synchronized
    fun snapshot(): Snapshot = Snapshot(sessionId, conversationContext)

    /**
     * Fold the server's answer back in.
     *
     * Only the summary is taken. The sent-reply list stays locally authoritative
     * because the server merely echoes the one we sent it: a reply the user
     * picked *while this round was in flight* is already recorded here, and
     * overwriting it with the echo would lose it.
     *
     * A stale [sessionId] — the user hit "+" mid-round — drops the answer.
     */
    @Synchronized
    fun update(sessionId: String, context: ConversationContext) {
        if (sessionId != this.sessionId) return
        conversationContext = conversationContext.copy(summary = context.summary)
    }

    /** Record a reply the user actually picked & sent. */
    @Synchronized
    fun recordSentReply(reply: String) {
        conversationContext = conversationContext.copy(
            sentReplies = (conversationContext.sentReplies + reply).takeLast(MAX_SENT_REPLIES)
        )
    }

    /** Start a fresh session: wipe the hidden context and mint a new session id. */
    @Synchronized
    fun reset() {
        conversationContext = ConversationContext()
        sessionId = UUID.randomUUID().toString()
    }
}
