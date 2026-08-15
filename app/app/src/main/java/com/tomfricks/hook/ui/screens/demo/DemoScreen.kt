package com.tomfricks.hook.ui.screens.demo

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.tomfricks.hook.ui.screens.onboarding.DemoChatStep

/**
 * "How to Use", reached from Home.
 *
 * This is the same practice run the user did during onboarding — the stand-in
 * chat, the real keyboard, a real generation — rather than a second, slide-based
 * explanation of it. One screen to maintain, and the thing people come back here
 * to re-learn is the doing, not the reading.
 *
 * The only difference is that this one can be left: [DemoChatStep] takes an
 * `onExit` and wires it to the header's back arrow and the switch-keyboard
 * scrim. Sending a reply ends it the same way onboarding does, which here means
 * going back to Home.
 */
@Composable
fun DemoScreen(onNavigateBack: () -> Unit) {
    BackHandler(onBack = onNavigateBack)

    DemoChatStep(
        onFinished = onNavigateBack,
        onExit = onNavigateBack
    )
}
