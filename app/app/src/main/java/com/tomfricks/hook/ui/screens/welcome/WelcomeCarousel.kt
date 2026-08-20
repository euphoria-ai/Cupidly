package com.tomfricks.hook.ui.screens.welcome

import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.tomfricks.hook.R
import com.tomfricks.hook.ui.theme.HookMarkFlat
import com.tomfricks.hook.ui.theme.PebbleButton
import com.tomfricks.hook.ui.theme.PebbleTone
import kotlinx.coroutines.launch

/**
 * The three slides shown before onboarding proper — the pitch, not the setup.
 *
 * Each is one picture and one promise: media on top, a two-line headline under
 * it, dots, and a single full-width action. Nothing is tappable except that
 * action, so there is one way forward and swiping is the shortcut.
 */
private data class WelcomeSlide(
    val headline: String,
    val action: String,
    /**
     * The media for this slide. All-null renders the placeholder frame, so the
     * flow stays walkable if art is ever missing.
     *
     * [image] is drawn bare — those assets carry their own device frame, so
     * wrapping them in [SlideFrame] would draw a second one around the first.
     * [video] is a raw screen recording and does get the frame.
     */
    @DrawableRes val image: Int? = null,
    @RawRes val video: Int? = null
)

private val Slides = listOf(
    WelcomeSlide(
        headline = "Get more matches.\nLand more dates.",
        action = "Get Started",
        image = R.drawable.welcome_matches
    ),
    WelcomeSlide(
        headline = "Hook is built directly\ninto your keyboard",
        action = "Continue",
        video = R.raw.welcome_keyboard
    ),
    WelcomeSlide(
        headline = "Craft the perfect opener\nto send on dating apps",
        action = "Continue",
        video = R.raw.welcome_opener
    )
)

/** Aspect of the bare slide-one artwork, which already includes its own frame. */
private const val ImageAspect = 376f / 668f

/** Aspect of the phone-shaped frame the demo recordings sit inside. */
private const val FrameAspect = 0.5f

/**
 * @param onFinished the last slide's action — hands over to onboarding, where
 *   the keyboard actually gets enabled.
 */
@Composable
fun WelcomeCarousel(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { Slides.size })
    val scope = rememberCoroutineScope()
    val slide = Slides[pagerState.currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            // The pager composes a neighbour ahead of time; only the slide
            // actually on screen should be playing sound-free video.
            SlidePage(slide = Slides[page], playing = page == pagerState.currentPage)
        }

        PageDots(
            count = Slides.size,
            current = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // One action, and its label is the slide's — "Get Started" only reads
        // right on the first one.
        PebbleButton(
            text = slide.action,
            tone = PebbleTone.SLATE,
            onClick = {
                val next = pagerState.currentPage + 1
                if (next < Slides.size) {
                    scope.launch { pagerState.animateScrollToPage(next) }
                } else {
                    onFinished()
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SlidePage(slide: WelcomeSlide, playing: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Height is the fixed side for every slide: let the phone shape derive
        // its width from it, or a wide screen stretches it into a slab.
        val mediaModifier = Modifier.fillMaxHeight(0.62f)

        when {
            slide.image != null -> Image(
                painter = painterResource(id = slide.image),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = mediaModifier
                    .aspectRatio(ratio = ImageAspect, matchHeightConstraintsFirst = true)
            )

            slide.video != null -> SlideFrame(
                modifier = mediaModifier
                    .aspectRatio(ratio = FrameAspect, matchHeightConstraintsFirst = true)
            ) {
                SlideVideo(
                    video = slide.video,
                    playing = playing,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> SlideFrame(
                modifier = mediaModifier
                    .aspectRatio(ratio = FrameAspect, matchHeightConstraintsFirst = true)
            ) {
                HookMarkFlat(size = 72.dp, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = slide.headline,
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The phone-shaped frame a screen recording sits inside, so it reads as a phone
 * rather than as a floating rectangle.
 */
@Composable
private fun SlideFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = shape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * A silent, looping demo clip that fills its frame edge to edge.
 *
 * ZOOM is the video equivalent of [ContentScale.Crop]: the clip is scaled until
 * both sides cover the frame and the overhang is clipped, so there is never a
 * letterbox gap between the recording and the frame around it.
 */
@OptIn(UnstableApi::class)
@Composable
private fun SlideVideo(
    @RawRes video: Int,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val player = remember(video) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri("android.resource://${context.packageName}/$video".toUri()))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            prepare()
        }
    }

    // Off-screen pages stay composed, and a backgrounded app must not keep
    // decoding frames; both are just "don't play right now".
    DisposableEffect(player, lifecycleOwner, playing) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> player.playWhenReady = playing
                Lifecycle.Event.ON_PAUSE -> player.playWhenReady = false
                else -> Unit
            }
        }
        player.playWhenReady = playing &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}

@Composable
private fun PageDots(
    count: Int,
    current: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val alpha by animateFloatAsState(
                targetValue = if (index == current) 1f else 0.22f,
                animationSpec = tween(250),
                label = "dot"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha),
                        shape = CircleShape
                    )
            )
        }
    }
}
