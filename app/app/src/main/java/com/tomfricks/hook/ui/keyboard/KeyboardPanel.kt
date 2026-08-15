package com.tomfricks.hook.ui.keyboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomfricks.hook.keyboard.RizzSession
import com.tomfricks.hook.keyboard.RizzSession.TranscriptItem
import com.tomfricks.hook.ui.theme.HookMarkFlat
import com.tomfricks.hook.ui.theme.PebbleActionPill
import com.tomfricks.hook.ui.theme.PebbleBubble
import com.tomfricks.hook.ui.theme.PebbleIconButton
import com.tomfricks.hook.ui.theme.TypingBubble

/** Height of the keyboard surface — tall enough for a readable screenshot. */
private val PanelHeight = 300.dp

/**
 * The whole keyboard surface, laid out as a chat you can follow up in: each
 * captured screenshot sits on the left, replies land under it on the right, and
 * new screenshots append below as an ongoing thread.
 *
 * Generation has no loading screen on purpose — the transcript stays on screen
 * and an animated typing bubble shows at the bottom while replies are written.
 *
 * Rendered by the IME straight off [RizzSession], so replies that arrived while
 * this keyboard did not exist are already here.
 */
@Composable
fun KeyboardPanel(
    state: RizzSession.Status,
    items: List<TranscriptItem>,
    errorMessage: String?,
    isDarkTheme: Boolean,
    onGenerate: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPro: Boolean = false,
    freeRemaining: Int = 0,
    paywallRequired: Boolean = false,
    onUpgradeClick: () -> Unit = {}
) {
    val hasReplies = items.any {
        it is TranscriptItem.SentReply || it is TranscriptItem.Suggestion
    }

    // Pro is never nagged and never blocked.
    val showUpsell = paywallRequired && !isPro

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(PanelHeight)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (!isPro && !showUpsell) {
            AllowanceChip(
                remaining = freeRemaining,
                onClick = onUpgradeClick
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (items.isEmpty()) {
                // Nothing to talk about yet: no chat, no screenshot card — just
                // the logo and what to do next.
                EmptyState(
                    isDarkTheme = isDarkTheme,
                    title = if (state == RizzSession.Status.ERROR) {
                        "Couldn't read that one"
                    } else {
                        "Take a screenshot"
                    },
                    body = if (state == RizzSession.Status.ERROR) {
                        errorMessage ?: "Something went wrong"
                    } else {
                        "So Hook knows what's on your screen"
                    }
                )
            } else {
                ChatTranscript(
                    transcript = items,
                    state = state,
                    errorMessage = errorMessage,
                    isDarkTheme = isDarkTheme,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }

        // The controls rest on a dark, semi-transparent fade that rises off the
        // bottom of the chat, so the "Generate rizz" pill and its neighbours read
        // clearly over the transcript without a hard band.
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            Column {
                if (showUpsell) {
                    // Sits where the fade normally is, so the panel keeps its
                    // height and the transcript stays readable behind it.
                    Text(
                        text = "You're out of free rizz. Go Pro for unlimited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(28.dp)
                    )
                } else {
                    // Fade zone above the bar — this is the "bit of chat" the
                    // scrim covers before darkening behind the controls.
                    Spacer(modifier = Modifier.height(28.dp))
                }

                KeyboardBottomBar(
                    state = state,
                    hasReplies = hasReplies,
                    showUpsell = showUpsell,
                    onGenerate = onGenerate,
                    onUpgradeClick = onUpgradeClick,
                    onNewChatClick = onNewChatClick,
                    onSettingsClick = onSettingsClick,
                    onBackspaceClick = onBackspaceClick
                )
            }
        }
    }
}

/**
 * The free-generation counter for non-Pro users, tucked into the top-right of
 * the panel. Tapping it opens the paywall in the main app.
 */
@Composable
private fun AllowanceChip(
    remaining: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (remaining > 0) "$remaining free left" else "Go Pro",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(shape)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = shape
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * A captured screenshot, shown as an attachment card in the chat — the way a
 * quoted image sits above the replies in a messaging app.
 */
@Composable
private fun ScreenshotCard(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: (@Composable () -> Unit)? = null
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
            HookMarkFlat(size = 52.dp, contentDescription = "No screenshot yet")
        }
    }
}

/**
 * One vertical chat rendered from the ordered session transcript: screenshots on
 * the left, sent replies and tappable suggestions on the right, and the typing
 * bubble while replies are being written. Multiple screenshots interleave with
 * replies so follow-ups read as one ongoing thread.
 */
@Composable
private fun ChatTranscript(
    transcript: List<TranscriptItem>,
    state: RizzSession.Status,
    errorMessage: String?,
    isDarkTheme: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val showError = state == RizzSession.Status.ERROR && errorMessage != null

    LaunchedEffect(transcript.size, showError) {
        val count = transcript.size + if (showError) 1 else 0
        if (count > 0) {
            listState.animateScrollToItem(count)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
        items(transcript) { item ->
            when (item) {
                is TranscriptItem.Screenshot -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    ScreenshotCard(isDarkTheme = isDarkTheme) {
                        // Anchor the crop to the top so the start of the
                        // conversation is visible instead of the middle.
                        Image(
                            bitmap = item.bitmap.asImageBitmap(),
                            contentDescription = "Captured screenshot",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                is TranscriptItem.SentReply -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PebbleBubble(
                        text = item.text,
                        onClick = null,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }

                is TranscriptItem.Suggestion -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    PebbleBubble(
                        text = item.text,
                        onClick = { onSuggestionClick(item.text) },
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }

                TranscriptItem.Typing -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TypingBubble()
                }
            }
        }

        if (showError) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.widthIn(max = 240.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardBottomBar(
    state: RizzSession.Status,
    hasReplies: Boolean,
    showUpsell: Boolean,
    onGenerate: () -> Unit,
    onUpgradeClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    val isGenerating = state == RizzSession.Status.GENERATING

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "+" clears the chat and starts a fresh hidden session.
        PebbleIconButton(
            icon = Icons.Default.Add,
            contentDescription = "New chat",
            onClick = onNewChatClick
        )

        PebbleActionPill(
            text = when {
                showUpsell -> "Unlock unlimited rizz"
                isGenerating -> "Cooking rizz…"
                hasReplies -> "Generate more rizz"
                else -> "Generate rizz"
            },
            // An IME can't host the Play purchase sheet, so this hands the user
            // to MainActivity on the paywall route instead.
            onClick = if (showUpsell) onUpgradeClick else onGenerate,
            // Locked while a round is cooking so it can't be fired twice.
            enabled = showUpsell || !isGenerating,
            modifier = Modifier.weight(1f)
        )

        // Settings moved to their own gear now that "+" starts a new chat.
        PebbleIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Hook settings",
            onClick = onSettingsClick
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
            HookMarkFlat(size = 48.dp)
        }
    }
}
