package com.tomlin7.cupidly.ui.keyboard

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomlin7.cupidly.R
import com.tomlin7.cupidly.keyboard.CupidlyKeyboardService.KeyboardState
import com.tomlin7.cupidly.ui.theme.IncomingBubble
import com.tomlin7.cupidly.ui.theme.PebbleActionPill
import com.tomlin7.cupidly.ui.theme.PebbleBubble
import com.tomlin7.cupidly.ui.theme.PebbleIconButton
import com.tomlin7.cupidly.ui.theme.PebbleTone

/** One line of the keyboard's chat transcript. */
data class KeyboardMessage(
    val text: String,
    val outgoing: Boolean,
    /** Sent messages are committed already and are not tappable. */
    val sent: Boolean = false
)

/**
 * The whole keyboard surface, laid out as a chat you can follow up in:
 * the captured screenshot sits small on the left, and the transcript of
 * suggestions (plus anything already sent) scrolls on the right.
 *
 * Rendered by the IME and, with canned data, by the in-app demo screen —
 * so both stay pixel-identical.
 */
@Composable
fun KeyboardPanel(
    state: KeyboardState,
    messages: List<KeyboardMessage>,
    errorMessage: String?,
    isDarkTheme: Boolean,
    onGenerate: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnail: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 190.dp, max = 260.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScreenshotThumbnail(
                isDarkTheme = isDarkTheme,
                content = thumbnail,
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxSize()
            )

            Box(modifier = Modifier.weight(1f)) {
                when (state) {
                    KeyboardState.SHOWING_SUGGESTIONS -> ChatTranscript(
                        messages = messages,
                        onSuggestionClick = onSuggestionClick
                    )

                    KeyboardState.COOKING -> CookingPane(isDarkTheme = isDarkTheme)

                    KeyboardState.CAPTURING -> LoadingPane("Capturing screenshot…")

                    KeyboardState.GENERATING -> LoadingPane("Generating replies…")

                    KeyboardState.ERROR -> MessagePane(
                        title = "No screenshot yet",
                        body = errorMessage ?: "Something went wrong"
                    )

                    KeyboardState.IDLE -> MessagePane(
                        title = "Take a screenshot",
                        body = "So Cupidly knows what's on your screen — then tap Generate rizz."
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        KeyboardBottomBar(
            state = state,
            onGenerate = onGenerate,
            onSettingsClick = onSettingsClick,
            onSwitchKeyboard = onSwitchKeyboard,
            onBackspaceClick = onBackspaceClick
        )
    }
}

@Composable
private fun ScreenshotThumbnail(
    isDarkTheme: Boolean,
    content: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
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
                modifier = Modifier.size(44.dp)
            )
        }
    }
}

@Composable
private fun ChatTranscript(
    messages: List<KeyboardMessage>,
    onSuggestionClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.End
    ) {
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
    onGenerate: () -> Unit,
    onSettingsClick: () -> Unit,
    onSwitchKeyboard: () -> Unit,
    onBackspaceClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PebbleIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Cupidly settings",
            onClick = onSettingsClick
        )

        PebbleIconButton(
            icon = Icons.Default.Keyboard,
            contentDescription = "Switch keyboard",
            onClick = onSwitchKeyboard
        )

        PebbleActionPill(
            text = when (state) {
                KeyboardState.SHOWING_SUGGESTIONS -> "Generate more rizz"
                else -> "Generate rizz"
            },
            onClick = onGenerate,
            modifier = Modifier.weight(1f)
        )

        PebbleIconButton(
            icon = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            onClick = onBackspaceClick,
            tone = PebbleTone.MUTED
        )
    }
}

@Composable
private fun LoadingPane(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CookingPane(isDarkTheme: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "cooking")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart_scale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                id = if (isDarkTheme) R.drawable.heart_transparent
                else R.drawable.heart_transparent_light
            ),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Cooking…",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "generating some rizz for you",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MessagePane(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
