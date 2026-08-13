package com.tomfricks.hook.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomfricks.hook.ui.theme.PebbleButton
import com.tomfricks.hook.ui.theme.PebbleTone

/**
 * The beat after the demo: the user just sent an AI-written reply and it
 * landed. This is the only screen in onboarding that asks for nothing.
 *
 * Animated rather than static so it reads as applause — the headline springs
 * in, the mark keeps a slow pulse. The framed box is a placeholder for the
 * celebration GIF; dropping one in means replacing its contents and nothing
 * else.
 */
@Composable
fun SmoothStep(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "smooth_scale"
    )
    val fade by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "smooth_fade"
    )

    val pulse = rememberInfiniteTransition(label = "smooth_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "smooth_pulse_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "That was smooth.",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .scale(scale)
                .alpha(fade)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "That's every conversation from here on.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(fade)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Placeholder for the celebration GIF.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .scale(pulseScale)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(24.dp)
                )
                .alpha(fade),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "😎", fontSize = 64.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        PebbleButton(
            text = "Continue",
            tone = PebbleTone.SLATE,
            onClick = onContinue
        )

        Spacer(modifier = Modifier.height(28.dp))
    }
}
