package com.tomfricks.cupidly.data

data class UserPreferences(
    val style: MessageStyle = MessageStyle.SENTENCE_CASE,
    val tone: MessageTone = MessageTone.SMOOTH,
    val flirtLevel: FlirtLevel = FlirtLevel.MEDIUM,
    val replyLength: ReplyLength = ReplyLength.NORMAL,
    val emojiUse: EmojiUse = EmojiUse.EXPRESSIVE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val profileName: String = "",
    val profileGender: String = "",
    val profileSexuality: String = "",
    val profileBio: String = "",
    val profilePronouns: String = "",
    val hasCompletedOnboarding: Boolean = false
)

enum class MessageStyle(val displayName: String) {
    LOWERCASE("lowercase"),
    SENTENCE_CASE("Sentence case")
}

enum class MessageTone(val displayName: String) {
    GEN_Z_SLANG("Gen-Z Slang"),
    RESPECTFUL("Respectful"),
    FUNNY("Funny"),
    SMOOTH("Smooth")
}

enum class FlirtLevel(val displayName: String) {
    LESS("Less"),
    MEDIUM("Moderate"),
    BOLD("Bold")
}

enum class ReplyLength(val displayName: String) {
    SHORT("Short"),
    NORMAL("Normal"),
    EXTENDED("Extended")
}

enum class EmojiUse(val displayName: String) {
    NEVER("Never"),
    MINIMAL("Minimal"),
    EXPRESSIVE("Expressive")
}

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}
