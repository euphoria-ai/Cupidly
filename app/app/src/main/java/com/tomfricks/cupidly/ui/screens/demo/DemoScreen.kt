package com.tomfricks.cupidly.ui.screens.demo

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomfricks.cupidly.ui.theme.IncomingBubble
import com.tomfricks.cupidly.ui.theme.PebbleBubble
import com.tomfricks.cupidly.ui.theme.PebbleButton
import com.tomfricks.cupidly.ui.theme.PebbleRow
import com.tomfricks.cupidly.ui.theme.PebbleSurface
import com.tomfricks.cupidly.ui.theme.PebbleTone
import kotlinx.coroutines.launch

private data class DemoLine(val text: String, val outgoing: Boolean)

/** The conversation waiting for you in the practice chat. */
private val demoChat = listOf(
    DemoLine("matched a week ago and you still haven't asked me out", outgoing = false),
    DemoLine("i was building suspense", outgoing = true),
    DemoLine("sure. the key to my heart is getting along with my dog, Karma", outgoing = false),
    DemoLine("she's a menace and she's the final boss", outgoing = false)
)

private data class DemoSlide(
    val step: String,
    val title: String,
    val body: String
)

private val demoSlides = listOf(
    DemoSlide(
        step = "1",
        title = "Switch to the Cupidly keyboard",
        body = "In the practice chat, tap the message box, then use the keyboard switch " +
            "button (or the globe key) and pick Cupidly Keyboard."
    ),
    DemoSlide(
        step = "2",
        title = "Screenshot the chat",
        body = "Power + Volume Down. Cupidly reads what's on screen — that's how it knows " +
            "who you're talking to and what they just said."
    ),
    DemoSlide(
        step = "3",
        title = "Tap Generate rizz",
        body = "Replies appear as a chat inside the keyboard. Tap one and it drops straight " +
            "into the message box. Tap Generate more rizz for another round."
    )
)

private enum class DemoStage { SLIDES, CHAT }

/**
 * Demo flow: a short how-to, then a real chat screen. The keyboard here is the
 * system keyboard — the user switches to Cupidly themselves and sends for real,
 * exactly as they would in any messaging app.
 */
@Composable
fun DemoScreen(onNavigateBack: () -> Unit) {
    var stage by remember { mutableStateOf(DemoStage.SLIDES) }

    when (stage) {
        DemoStage.SLIDES -> DemoSlides(
            onNavigateBack = onNavigateBack,
            onStart = { stage = DemoStage.CHAT }
        )

        DemoStage.CHAT -> DemoChat(
            onNavigateBack = { stage = DemoStage.SLIDES }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoSlides(
    onNavigateBack: () -> Unit,
    onStart: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { demoSlides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == demoSlides.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DemoTopBar(title = "How the demo works", onNavigateBack = onNavigateBack)

        // Page indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(demoSlides.size) { index ->
                val active = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (active) 24.dp else 8.dp,
                    animationSpec = tween(300),
                    label = "indicator_width"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (active) 1f else 0.3f,
                    animationSpec = tween(300),
                    label = "indicator_alpha"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(width)
                        .height(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            shape = CircleShape
                        )
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val slide = demoSlides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                PebbleSurface(
                    onClick = null,
                    modifier = Modifier.size(72.dp),
                    tone = PebbleTone.BLUE,
                    cornerRadius = 26.dp
                ) {
                    Text(
                        text = slide.step,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            PebbleButton(
                text = if (isLastPage) "Start the demo" else "Next",
                onClick = {
                    if (isLastPage) {
                        onStart()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
            if (!isLastPage) {
                Spacer(modifier = Modifier.height(10.dp))
                PebbleButton(
                    text = "Skip to the chat",
                    onClick = onStart,
                    tone = PebbleTone.MUTED
                )
            }
        }
    }
}

@Composable
private fun DemoChat(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var sent by remember { mutableStateOf<List<String>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    val lines = demoChat + sent.map { DemoLine(it, outgoing = true) }

    LaunchedEffect(lines.size) {
        listState.animateScrollToItem(lines.size)
    }

    // Open the keyboard straight away so the switch key is one tap away.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun send() {
        val text = draft.trim()
        if (text.isNotEmpty()) {
            sent = sent + text
            draft = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        DemoTopBar(title = "Priya · matched 1w ago", onNavigateBack = onNavigateBack)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                PebbleRow(
                    title = "Screenshot this chat, then tap Generate rizz",
                    subtitle = "Switch to the Cupidly keyboard first — the button is below.",
                    tone = PebbleTone.MUTED
                )
            }

            items(lines) { line ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (line.outgoing) Arrangement.End else Arrangement.Start
                ) {
                    if (line.outgoing) {
                        PebbleBubble(
                            text = line.text,
                            onClick = null,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    } else {
                        IncomingBubble(
                            text = line.text,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                }
            }
        }

        // Real message box — the system keyboard drives this, not a mock.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE)
                        as InputMethodManager
                    imm.showInputMethodPicker()
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Switch keyboard",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text("Message") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            PebbleSurface(
                onClick = { send() },
                modifier = Modifier.size(46.dp),
                tone = PebbleTone.BLUE,
                cornerRadius = 23.dp,
                elevation = 4.dp
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DemoTopBar(title: String, onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
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
}
