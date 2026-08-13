package com.tomfricks.hook.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomfricks.hook.keyboard.ConversationSession
import com.tomfricks.hook.keyboard.HookKeyboardService
import com.tomfricks.hook.keyboard.RizzSession
import com.tomfricks.hook.utils.PermissionUtils
import kotlinx.coroutines.delay

/**
 * The try-it-out step: a stand-in dating-app chat the user actually uses Hook
 * on, with the real keyboard, the real screenshot detection and a real
 * generation. Nothing here is faked except the conversation.
 *
 * It runs itself off the same session state the keyboard writes to, so the
 * coaching line always describes what is genuinely true right now:
 *
 *  1. switch to Hook (dimmed until they do — this is the step people get stuck on)
 *  2. take a screenshot
 *  3. tap one of the replies Hook wrote
 *  4. send it, and watch it land in the thread
 *
 * The chat is deliberately plain Instagram colours rather than Hook's theme:
 * the point is that this looks like the app they'll really be typing into.
 */
@Composable
fun DemoChatStep(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardShowing by HookKeyboardService.isShowing.collectAsState()

    var messages by remember { mutableStateOf(DemoThread) }
    var draft by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // The demo generates for real, so it starts from a clean session — and
    // leaves one behind, so the user's first real screenshot isn't answered in
    // the context of a conversation with someone who doesn't exist.
    LaunchedEffect(Unit) {
        RizzSession.reset()
        ConversationSession.reset()
        focusRequester.requestFocus()
    }

    val phase = when {
        !keyboardShowing -> DemoPhase.SWITCH_KEYBOARD
        sent -> DemoPhase.SENT
        draft.isNotBlank() -> DemoPhase.SEND_IT
        RizzSession.hasPendingSuggestions -> DemoPhase.PICK_REPLY
        RizzSession.status == RizzSession.Status.GENERATING -> DemoPhase.WORKING
        else -> DemoPhase.SCREENSHOT
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    // This step is a checkpoint with no way back, so it must always have a way
    // *forward*. Generation can fail, the free allowance can be spent, a device
    // can put the screenshot somewhere we never see — none of which the user
    // can do anything about, and none of which should end their onboarding.
    var showSkip by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(25_000)
        showSkip = true
    }
    val stuck = showSkip || RizzSession.error != null || RizzSession.paywallRequired

    // Let the sent message land, then move on — asking for a tap here would
    // just be a button between them and the applause.
    LaunchedEffect(sent) {
        if (sent) {
            delay(1100)
            RizzSession.reset()
            ConversationSession.reset()
            onFinished()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(InstagramWhite)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            ChatHeader()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(messages) { _, message ->
                    ChatBubble(message)
                }
            }

            Coach(phase = phase)

            if (stuck && !sent) {
                Text(
                    text = "Skip this for now",
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = InstagramGrayText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            RizzSession.reset()
                            ConversationSession.reset()
                            onFinished()
                        }
                        .padding(vertical = 10.dp)
                )
            }

            ChatComposer(
                draft = draft,
                onDraftChange = { draft = it },
                focusRequester = focusRequester,
                onSend = {
                    if (draft.isNotBlank()) {
                        messages = messages + DemoMessage(draft.trim(), fromMe = true)
                        draft = ""
                        sent = true
                    }
                }
            )
        }

        // Everything above is unusable until the user is actually on the Hook
        // keyboard, because every later instruction depends on it.
        AnimatedVisibility(
            visible = phase == DemoPhase.SWITCH_KEYBOARD,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SwitchKeyboardScrim(onOpenPicker = { PermissionUtils.showKeyboardPicker(context) })
        }
    }
}

private enum class DemoPhase {
    SWITCH_KEYBOARD,
    SCREENSHOT,
    WORKING,
    PICK_REPLY,
    SEND_IT,
    SENT
}

/** The dark instruction pill that sits just above the composer. */
@Composable
private fun Coach(phase: DemoPhase) {
    val text = when (phase) {
        DemoPhase.SWITCH_KEYBOARD -> null
        DemoPhase.SCREENSHOT -> "Take a screenshot (and save it)"
        DemoPhase.WORKING -> "Reading the conversation…"
        DemoPhase.PICK_REPLY -> "Choose a message to send!"
        DemoPhase.SEND_IT -> "Now hit send"
        DemoPhase.SENT -> "That's it — that's the whole app"
    } ?: return

    Text(
        text = text,
        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(InstagramCoach)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    )
}

/**
 * Covers the demo until Hook is the keyboard on screen.
 *
 * The placeholder box is where the how-to-switch GIF goes; the button is the
 * reliable way to do it on Android, since the globe key isn't on every keyboard.
 */
@Composable
private fun SwitchKeyboardScrim(onOpenPicker: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            // Swallow every tap — nothing behind this is usable yet — but
            // without a full-screen ripple answering each one.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Switch to the Hook keyboard",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tap and hold the 🌐 key, or the space bar, and pick Hook.",
                style = TextStyle(fontSize = 15.sp),
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Placeholder for the how-to-switch GIF.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌐",
                    style = TextStyle(fontSize = 44.sp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Show keyboard chooser",
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = InstagramCoach,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .clickable(onClick = onOpenPicker)
                    .padding(horizontal = 28.dp, vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun ChatHeader() {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = DemoMatchName,
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Chat",
                    style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    color = InstagramPurple
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(96.dp)
                        .height(2.dp)
                        .background(InstagramPurple)
                )
            }
            Text(
                text = "/",
                style = TextStyle(fontSize = 17.sp),
                color = InstagramGrayText,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
            )
            Text(
                text = "Profile",
                style = TextStyle(fontSize = 17.sp),
                color = InstagramGrayText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(InstagramDivider)
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun ChatBubble(message: DemoMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!message.fromMe) {
            // Stand-in for the match's photo — a real one would have to come
            // from somewhere, and this is a conversation that never happened.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(InstagramPurple.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = DemoMatchName.take(1),
                    style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = InstagramPurple
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = message.text,
            style = TextStyle(fontSize = 17.sp, lineHeight = 23.sp),
            color = if (message.fromMe) Color.White else Color.Black,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (message.fromMe) InstagramPurple else InstagramIncoming)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/**
 * The message box. It is a real, focused field — that's what brings the
 * keyboard up, and what a tapped suggestion is committed into.
 */
@Composable
private fun ChatComposer(
    draft: String,
    onDraftChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = InstagramDivider,
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            if (draft.isEmpty()) {
                Text(
                    text = "Send a message",
                    style = TextStyle(fontSize = 17.sp),
                    color = InstagramGrayText
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = onDraftChange,
                textStyle = TextStyle(fontSize = 17.sp, color = Color.Black),
                cursorBrush = SolidColor(InstagramPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        val ready = draft.isNotBlank()
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (ready) InstagramPurple else InstagramIncoming)
                .clickable(enabled = ready, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (ready) Color.White else InstagramGrayText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class DemoMessage(val text: String, val fromMe: Boolean)

private const val DemoMatchName = "Jessica"

/**
 * The conversation the user practises on. It ends on her question, so the reply
 * Hook writes is the obvious next thing to say.
 */
private val DemoThread = listOf(
    DemoMessage("I'm scared you might distract me at the gym 😏", fromMe = true),
    DemoMessage("You must see something you like… 👀", fromMe = false),
    DemoMessage("Question is: can you handle it if I give it my full attention?", fromMe = true),
    DemoMessage("Do you think you can handle me?", fromMe = false)
)

// Fixed colours, not the app theme: this screen is pretending to be somebody
// else's app, and it looks the same in dark mode for the same reason.
private val InstagramWhite = Color(0xFFFFFFFF)
private val InstagramPurple = Color(0xFF6E2A63)
private val InstagramIncoming = Color(0xFFEFEFEF)
private val InstagramGrayText = Color(0xFF8E8E8E)
private val InstagramDivider = Color(0xFFDBDBDB)
private val InstagramCoach = Color(0xFF3A3A3A)
