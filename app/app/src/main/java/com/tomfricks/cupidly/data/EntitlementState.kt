package com.tomfricks.cupidly.data

/**
 * Cached entitlement + free-allowance state for this install.
 *
 * Mirrors the fields the server hands back on `/generate-replies` and `/me`, and
 * is persisted so surfaces that render before any network call (the keyboard's
 * "3 free left" chip, the paywall entry point) already have the right answer.
 *
 * The money model is "5 lifetime free generations, then a subscription": the
 * server is the source of truth, this is only the last thing it told us.
 */
data class EntitlementState(
    val isPro: Boolean = false,
    val freeUsed: Int = 0,
    val freeLimit: Int = PreferencesRepository.DEFAULT_FREE_LIMIT
) {
    /** Free generations left. Meaningless for Pro, who are unlimited. */
    val remaining: Int
        get() = (freeLimit - freeUsed).coerceAtLeast(0)

    /** True when a non-Pro user has nothing left and must see the paywall. */
    val isExhausted: Boolean
        get() = !isPro && remaining <= 0
}
