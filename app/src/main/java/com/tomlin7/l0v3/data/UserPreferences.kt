package com.tomlin7.l0v3.data

data class UserPreferences(
    val style: MessageStyle = MessageStyle.SENTENCE_CASE,
    val tone: MessageTone = MessageTone.SMOOTH,
    val flirtLevel: FlirtLevel = FlirtLevel.MEDIUM,
    val replyLength: ReplyLength = ReplyLength.NORMAL,
    val emojiUse: EmojiUse = EmojiUse.EXPRESSIVE,
    val profileName: String = "",
    val profileGender: String = "",
    val profileSexuality: String = "",
    val profileBio: String = "",
    val profilePronouns: String = "",
    val geminiApiKey: String = "",
    val hasCompletedOnboarding: Boolean = false
)

enum class MessageStyle(val displayName: String) {
    LOWERCASE("lowercase"),
    SENTENCE_CASE("Sentence case"),
    EMOJI_RICH("emoji-rich 🎉"),
    POETIC("Poetic")
}

enum class MessageTone(val displayName: String) {
    GEN_Z_SLANG("Gen-Z Slang"),
    RESPECTFUL("Respectful"),
    FUNNY("Funny"),
    SMOOTH("Smooth")
}

enum class FlirtLevel(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    BOLD("Bold")
}

enum class ReplyLength(val displayName: String) {
    SHORT("Short"),
    NORMAL("Normal"),
    EXTENDED("Extended")
}

enum class EmojiUse(val displayName: String) {
    MINIMAL("Minimal"),
    EXPRESSIVE("Expressive")
}
