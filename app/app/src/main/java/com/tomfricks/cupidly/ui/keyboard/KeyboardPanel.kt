package com.tomfricks.cupidly.ui.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomfricks.cupidly.R
import com.tomfricks.cupidly.keyboard.CupidlyKeyboardService.KeyboardState
import com.tomfricks.cupidly.ui.theme.IncomingBubble
import com.tomfricks.cupidly.ui.theme.PebbleActionPill
import com.tomfricks.cupidly.ui.theme.PebbleBubble
import com.tomfricks.cupidly.ui.theme.PebbleIconButton

/** One line of the keyboard's chat transcript. */
data class KeyboardMessage(
    val text: String,
    val outgoing: Boolean,
    /** Sent messages are committed already and are not tappable. */
    val sent: Boolean = false
)

/** Height of the keyboard surface — tall enough for a readable screenshot. */
private val PanelHeight = 300.dp

/**
 * The whole keyboard surface, laid out as a chat you can follow up in:
 * the captured screenshot sits on the left, and the transcript of suggestions
 * (plus anything already sent) scrolls on the right.
 *
 * Generation has no loading screen on purpose — the transcript stays on screen
 * and new suggestions simply appear in it.
 *
 * Rendered by the IME and, with a real screenshot, by the in-app demo screen —
 * so both are the same keyboard.
 */
@Composable
fun KeyboardPanel(
    state: KeyboardState,
    messages: List<KeyboardMessage>,
    errorMessage: String?,
    isDarkTheme: Boolean,
    onGenerate: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onPlusClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (messages.isEmpty()) {
                // Nothing to talk about yet: no chat, no screenshot card — just
                // the logo and what to do next.
                EmptyState(
                    isDarkTheme = isDarkTheme,
                    title = when (state) {
                        KeyboardState.ERROR -> "Couldn't read that one"
                        KeyboardState.GENERATING, KeyboardState.COOKING -> "Cooking rizz…"
                        else -> "Take a screenshot"
                    },
                    body = when (state) {
                        KeyboardState.ERROR -> errorMessage ?: "Something went wrong"
                        KeyboardState.GENERATING, KeyboardState.COOKING ->
                            "Reading the chat and writing your replies"

                        else -> "So Cupidly knows what's on your screen"
                    }
                )
            } else {
                ChatTranscript(
                    messages = messages,
                    isDarkTheme = isDarkTheme,
                    thumbnail = thumbnail,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        KeyboardBottomBar(
            state = state,
            hasMessages = messages.isNotEmpty(),
            onGenerate = onGenerate,
            onPlusClick = onPlusClick,
            onBackspaceClick = onBackspaceClick
        )
    }
}

/**
 * The captured screenshot, shown as an attachment card at the top of the chat —
 * the way a quoted image sits above the replies in a messaging app.
 */
@Composable
private fun ScreenshotCard(
    isDarkTheme: Boolean,
    content: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .width(210.dp)
            .height(150.dp)
            .clip(shape)
            .background(
                color = if (isDarkTheme) Color(0xFF141B2B) else Color(0xFFEDF1F9),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else {
            Image(
                painter = painterResource(
                    id = if (isDarkTheme) R.drawable.heart_transparent
                    else R.drawable.heart_transparent_light
                ),
                contentDescription = "No screenshot yet",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

/**
 * One vertical chat: the screenshot card first, then every reply below it —
 * suggestions and already-sent messages alike.
 */
@Composable
private fun ChatTranscript(
    messages: List<KeyboardMessage>,
    isDarkTheme: Boolean,
    thumbnail: (@Composable () -> Unit)?,
    onSuggestionClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                ScreenshotCard(isDarkTheme = isDarkTheme, content = thumbnail)
            }
        }

        items(messages) { message ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (message.outgoing) Arrangement.End else Arrangement.Start
            ) {
                when {
                    !message.outgoing -> IncomingBubble(
                        text = message.text,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )

                    message.sent -> PebbleBubble(
                        text = message.text,
                        onClick = null,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )

                    else -> PebbleBubble(
                        text = message.text,
                        onClick = { onSuggestionClick(message.text) },
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardBottomBar(
    state: KeyboardState,
    hasMessages: Boolean,
    onGenerate: () -> Unit,
    onPlusClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PebbleIconButton(
            icon = Icons.Default.Add,
            contentDescription = "Cupidly settings",
            onClick = onPlusClick
        )

        PebbleActionPill(
            text = when {
                state == KeyboardState.GENERATING || state == KeyboardState.COOKING ->
                    "Cooking rizz…"

                hasMessages -> "Generate more rizz"
                else -> "Generate rizz"
            },
            onClick = onGenerate,
            modifier = Modifier.weight(1f)
        )

        PebbleIconButton(
            icon = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            onClick = onBackspaceClick
        )
    }
}

/**
 * Shown whenever there is nothing to reply to: instruction on top, app mark
 * below it on a soft tile. No chat, no screenshot card.
 */
@Composable
private fun EmptyState(
    isDarkTheme: Boolean,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .background(
                    color = if (isDarkTheme) Color(0xFF161D2E) else Color.White,
                    shape = RoundedCornerShape(22.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(22.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = if (isDarkTheme) R.drawable.heart_transparent
                    else R.drawable.heart_transparent_light
                ),
                contentDescription = "Cupidly",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
