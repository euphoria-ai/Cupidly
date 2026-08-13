package com.tomfricks.hook.data

/**
 * The parts of the profile the onboarding survey fills in.
 *
 * Answers are stored as the label the user actually tapped, so they can be
 * dropped into a prompt verbatim. Adding a question means adding a field here,
 * a key in [PreferencesRepository], and an entry in [OnboardingQuestions].
 */
enum class ProfileField {
    GENDER,
    SEXUALITY,
    AGE_RANGE,
    LOOKING_FOR
}

/**
 * One screen of the survey: a question, why we're asking, and the answers.
 *
 * Options are plain labels — the emoji in "🔥 Fun & hookups" is part of the
 * label, which keeps the row a single string and the list easy to edit.
 */
data class OnboardingQuestion(
    val field: ProfileField,
    val title: String,
    val subtitle: String,
    val options: List<String>
)

/**
 * The survey, in order. This list *is* the flow: the screen renders one entry
 * at a time and the progress bar reads its length, so more questions can be
 * added here alone.
 */
val OnboardingQuestions = listOf(
    OnboardingQuestion(
        field = ProfileField.GENDER,
        title = "What's your gender?",
        subtitle = "This will be used to cater your generated responses",
        options = listOf("Male", "Female", "Other")
    ),
    OnboardingQuestion(
        field = ProfileField.SEXUALITY,
        title = "What's your sexuality?",
        subtitle = "This will be used to cater your generated responses",
        options = listOf("Straight", "Gay", "Lesbian", "Bisexual")
    ),
    OnboardingQuestion(
        field = ProfileField.AGE_RANGE,
        title = "What's your age?",
        subtitle = "Depending on your age, we'll recommend a different app setup for you.",
        options = listOf(
            "Under 18",
            "18-24",
            "25-34",
            "35-44",
            "45-54",
            "55-64",
            "Over 64"
        )
    ),
    OnboardingQuestion(
        field = ProfileField.LOOKING_FOR,
        title = "I'm looking for...",
        subtitle = "This question ensures we help you attract compatible partners!",
        options = listOf(
            "💕 A relationship",
            "🔥 Fun & hookups",
            "🍷 Casual dates",
            "🤔 Still figuring it out"
        )
    )
)
