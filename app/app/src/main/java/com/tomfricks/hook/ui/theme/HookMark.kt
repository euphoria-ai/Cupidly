package com.tomfricks.hook.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tomfricks.hook.R

/**
 * The Hook app mark: the filled brand tile, lifted off the background.
 *
 * The two artworks are inverses of each other — a black tile with a white glyph
 * for light backgrounds, a white tile with a black glyph for dark ones — so the
 * only thing that ever picks between them is [LocalHookDarkTheme]. That is
 * deliberately *not* a `-night` resource qualifier: the Theme setting can force
 * Light or Dark against the system, and a qualifier would keep following the
 * system and show the inverted tile whenever the two disagree.
 *
 * The source art is a rounded tile on transparency. [CORNER_PERCENT] traces its
 * corners, so the shadow and the artwork share one silhouette. The tile colour
 * is painted underneath as well: the art is an iOS-style squircle and this is a
 * circular-arc approximation of it, so the two edges disagree by a pixel or so
 * near the corners — filling behind with the tile's own colour makes that
 * difference invisible instead of a fringe.
 */
@Composable
fun HookMark(
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Hook",
    elevation: Dp = 18.dp
) {
    val dark = LocalHookDarkTheme.current
    val shape = RoundedCornerShape(percent = CORNER_PERCENT)

    // A black shadow under a white tile on a near-black background is invisible,
    // so dark mode casts the Pebble blue instead and the lift reads as a glow.
    val shadowColor = if (dark) PebbleBlue else Color.Black

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(if (dark) Color.White else Color.Black, shape)
    ) {
        Image(
            painter = painterResource(
                id = if (dark) R.drawable.ic_hook_mark_dark else R.drawable.ic_hook_mark_light
            ),
            contentDescription = contentDescription,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * The mark on a card or key, where the surface already provides the elevation
 * and a second shadow would just muddy it.
 */
@Composable
fun HookMarkFlat(
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = "Hook"
) {
    HookMark(
        size = size,
        modifier = modifier,
        contentDescription = contentDescription,
        elevation = 0.dp
    )
}

/** Corner radius of the brand tile, as a percentage of its width. */
private const val CORNER_PERCENT = 25

/** Neutral tint for settings row icons — present, never competing with the value. */
val settingIconTint: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
