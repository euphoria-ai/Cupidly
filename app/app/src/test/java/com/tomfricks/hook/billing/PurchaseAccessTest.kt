package com.tomfricks.hook.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseAccessTest {

    @Test
    fun paywallWaitsForPlaySync() {
        assertFalse(shouldPresentPaywall(isPro = false, purchasesSynced = false))
    }

    @Test
    fun paywallOpensForFreeUsersAfterSync() {
        assertTrue(shouldPresentPaywall(isPro = false, purchasesSynced = true))
    }

    @Test
    fun paywallDoesNotOpenForSubscribers() {
        assertFalse(shouldPresentPaywall(isPro = true, purchasesSynced = true))
        assertFalse(shouldPresentPaywall(isPro = true, purchasesSynced = false))
    }
}
