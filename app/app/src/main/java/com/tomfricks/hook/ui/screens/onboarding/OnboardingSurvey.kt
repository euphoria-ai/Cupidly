package com.tomfricks.hook.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomfricks.hook.data.OnboardingQuestions
import com.tomfricks.hook.data.PreferencesRepository
import com.tomfricks.hook.data.ProfileField
import com.tomfricks.hook.ui.theme.PebbleButton
import com.tomfricks.hook.ui.theme.PebbleDialog
import com.tomfricks.hook.ui.theme.PebbleOption
import com.tomfricks.hook.ui.theme.PebbleTextButton
import com.tomfricks.hook.ui.theme.PebbleTone
import kotlinx.coroutines.launch

/**
 * The profile survey: one question per screen, answers kept in memory and
 * written in a single batch at the end.
 *
 * The flow is [OnboardingQuestions] — this screen renders whatever is in that
 * list, so questions are added there, not here.
 *
 * @param onFinished every question answered; the profile is already saved.
 * @param onBackFromStart back pressed on the first question — there's nothing
 *   behind it in this screen, so the caller decides where that goes.
 */
@Composable
fun OnboardingSurvey(
    onFinished: () -> Unit,
    onBackFromStart: () -> Unit,
    preferencesRepository: PreferencesRepository
) {
    val questions = OnboardingQuestions
    var step by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<ProfileField, String>() }
    var showWhyWeAsk by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val question = questions[step]
    val answer = answers[question.field]

    fun back() {
        if (step == 0) onBackFromStart() else step--
    }

    fun forward() {
        if (step < questions.lastIndex) {
            step++
        } else {
            // Save once, at the end: a profile half-written by a user who
            // wandered off mid-survey is worse than no profile at all.
            scope.launch {
                preferencesRepository.updateOnboardingProfile(answers.toMap())
                onFinished()
            }
        }
    }

    BackHandler { back() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SurveyTopBar(
            progress = (step + 1).toFloat() / questions.size,
            onBack = ::back,
            onHelp = { showWhyWeAsk = true }
        )

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = question.title,
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = question.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Scrolls on its own so a long list (the seven age brackets) never
        // pushes Continue off the bottom.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(question.options) { option ->
                PebbleOption(
                    text = option,
                    selected = option == answer,
                    selectedTone = PebbleTone.SLATE,
                    onClick = { answers[question.field] = option }
                )
            }
        }

        PebbleButton(
            text = "Continue",
            tone = PebbleTone.SLATE,
            // Nothing to continue *to* until the question is answered — the
            // survey's whole job is the answer.
            enabled = answer != null,
            onClick = ::forward,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))
    }

    if (showWhyWeAsk) {
        PebbleDialog(
            title = "Why we ask",
            subtitle = "Your answers shape the replies Hook writes for you — who " +
                "you are, who you're talking to, and how forward to be. They stay " +
                "on this device and in the replies we generate for you.",
            onDismiss = { showWhyWeAsk = false }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                PebbleTextButton(text = "Got it", onClick = { showWhyWeAsk = false })
            }
        }
    }
}

/** Back chevron, progress, and the "why are you asking me this" escape hatch. */
@Composable
private fun SurveyTopBar(
    progress: Float,
    onBack: () -> Unit,
    onHelp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onBack)
                .padding(4.dp)
                .size(24.dp)
        )

        ProgressTrack(
            progress = progress,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
            contentDescription = "Why we ask",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onHelp)
                .padding(4.dp)
                .size(24.dp)
        )
    }
}

/** The thin filled bar between the chevron and the help icon. */
@Composable
private fun ProgressTrack(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "progress"
    )
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = modifier
            .height(7.dp)
            .clip(shape)
            .background(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                shape = shape
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(7.dp)
                .background(color = MaterialTheme.colorScheme.onBackground, shape = shape)
        )
    }
}
