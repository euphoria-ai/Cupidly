package com.tomlin7.cupidly.ui.screens.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomlin7.cupidly.keyboard.CupidlyKeyboardService.KeyboardState
import com.tomlin7.cupidly.ui.keyboard.KeyboardMessage
import com.tomlin7.cupidly.ui.keyboard.KeyboardPanel
import com.tomlin7.cupidly.ui.theme.IncomingBubble
import com.tomlin7.cupidly.ui.theme.PebbleBubble
import com.tomlin7.cupidly.ui.theme.isPebbleDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Canned chat the demo pretends to have screenshotted. */
private val demoChat = listOf(
    KeyboardMessage("hey this is jessica from the pally party", outgoing = false),
    KeyboardMessage("jonathan liu", outgoing = true, sent = true),
    KeyboardMessage("nice meeting you today !", outgoing = true, sent = true),
    KeyboardMessage("you too haha i liked your dance moves", outgoing = false)
)

private val demoRounds = listOf(
    listOf(
        "marry me haha",
        "wanna do a diff kind of dance in my bedroom",
        "there's a lot more about me i think you'll like"
    ),
    listOf(
        "ok but you have to teach me your moves first",
        "next round: kitchen, 2am, terrible playlist",
        "i'm free friday if you want a rematch"
    ),
    listOf(
        "warning: i only know one move and it's bad",
        "dance battle, loser buys coffee",
        "you say that now, wait til you see the worm"
    )
)

/**
 * A sandbox that runs the real [KeyboardPanel] against a canned conversation,
 * so people can feel the keyboard before enabling it system-wide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(onNavigateBack: () -> Unit) {
    var state by remember { mutableStateOf(KeyboardState.IDLE) }
    var round by remember { mutableStateOf(0) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var sent by remember { mutableStateOf<List<String>>(emptyList()) }
    var typed by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun generate() {
        scope.launch {
            state = KeyboardState.COOKING
            delay(900)
            state = KeyboardState.GENERATING
            delay(700)
            suggestions = demoRounds[round % demoRounds.size]
            round += 1
            state = KeyboardState.SHOWING_SUGGESTIONS
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Demo",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Try it out",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "This is the real keyboard, wired to a fake chat. " +
                    "Tap Generate rizz, then tap a reply to send it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Fake conversation the "screenshot" was taken of.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Jessica (sf 8)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                (demoChat.map { it.text to it.outgoing } + sent.map { it to true })
                    .forEach { (text, outgoing) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start
                        ) {
                            if (outgoing) {
                                PebbleBubble(
                                    text = text,
                                    onClick = null,
                                    modifier = Modifier.widthIn(max = 260.dp)
                                )
                            } else {
                                IncomingBubble(
                                    text = text,
                                    modifier = Modifier.widthIn(max = 260.dp)
                                )
                            }
                        }
                    }

                if (typed.isNotEmpty()) {
                    Text(
                        text = "Draft: $typed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // The keyboard itself, pinned to the bottom like a real IME.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            KeyboardPanel(
                state = state,
                messages = suggestions.map { KeyboardMessage(it, outgoing = true) },
                errorMessage = null,
                isDarkTheme = isPebbleDark,
                onGenerate = { generate() },
                onSuggestionClick = { reply ->
                    sent = sent + reply
                    suggestions = suggestions.filterNot { it == reply }
                    typed = ""
                },
                onSettingsClick = onNavigateBack,
                onSwitchKeyboard = { },
                onBackspaceClick = {
                    typed = typed.dropLast(1)
                },
                thumbnail = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        demoChat.takeLast(3).forEach { message ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .background(
                                        color = if (message.outgoing) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                    }
                }
            )
        }
    }
}
