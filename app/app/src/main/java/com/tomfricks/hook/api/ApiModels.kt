package com.tomfricks.hook.api

import com.google.gson.annotations.SerializedName

data class GenerateRepliesRequest(
    @SerializedName("screenshot_base64")
    val screenshotBase64: String,
    @SerializedName("preferences")
    val preferences: PreferencesDto,
    @SerializedName("context")
    val context: ConversationContext? = null
)

/**
 * Hidden, session-only understanding of the whole conversation. Never shown in
 * any UI surface and never persisted to disk. Field names mirror the server.
 */
data class ConversationContext(
    @SerializedName("summary")
    val summary: String = "",
    @SerializedName("sent_replies")
    val sentReplies: List<String> = emptyList()
)

data class PreferencesDto(
    @SerializedName("style")
    val style: String,
    @SerializedName("tone")
    val tone: String,
    @SerializedName("flirt_level")
    val flirtLevel: String,
    @SerializedName("reply_length")
    val replyLength: String,
    @SerializedName("emoji_use")
    val emojiUse: String,
    @SerializedName("profile_name")
    val profileName: String = "",
    @SerializedName("profile_gender")
    val profileGender: String = "",
    @SerializedName("profile_pronouns")
    val profilePronouns: String = "",
    @SerializedName("profile_bio")
    val profileBio: String = ""
)

data class GenerateRepliesResponse(
    @SerializedName("suggestions")
    val suggestions: List<String>,
    @SerializedName("context")
    val context: ConversationContext = ConversationContext(),
    // Allowance state, so the paywall UI stays current without a /me round-trip.
    // `remaining` counts FREE generations only — ignore it when isPro is true.
    @SerializedName("is_pro")
    val isPro: Boolean = false,
    @SerializedName("free_used")
    val freeUsed: Int = 0,
    @SerializedName("free_limit")
    val freeLimit: Int = 0,
    @SerializedName("remaining")
    val remaining: Int = 0
)

/**
 * Body of `POST /onboarding` — what onboarding learned, sent once when the flow
 * finishes. Field names mirror the server's columns.
 */
data class OnboardingProfileRequest(
    @SerializedName("gender")
    val gender: String? = null,
    @SerializedName("sexuality")
    val sexuality: String? = null,
    @SerializedName("age_range")
    val ageRange: String? = null,
    @SerializedName("looking_for")
    val lookingFor: String? = null,
    @SerializedName("style")
    val style: String? = null,
    @SerializedName("tone")
    val tone: String? = null,
    @SerializedName("flirt_level")
    val flirtLevel: String? = null
)

/**
 * Body of `POST /report` — one generated reply a user flagged as offensive.
 *
 * Only the flagged text goes out. Not the screenshot it came from, and not the
 * conversation context: a report is about a single thing the model said.
 */
data class ContentReportRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("reason")
    val reason: String? = null
)

/** Body of `GET /me` — entitlement + allowance for this install. */
data class MeResponse(
    @SerializedName("app_user_id")
    val appUserId: String = "",
    @SerializedName("is_pro")
    val isPro: Boolean = false,
    @SerializedName("free_used")
    val freeUsed: Int = 0,
    @SerializedName("free_limit")
    val freeLimit: Int = 0,
    @SerializedName("remaining")
    val remaining: Int = 0
)

/**
 * Body of the HTTP 402 the server returns once the lifetime free allowance is
 * spent: `{"error": "allowance_exhausted", "free_limit": N, "used": M}`.
 */
data class AllowanceExhaustedResponse(
    @SerializedName("error")
    val error: String = "",
    @SerializedName("free_limit")
    val freeLimit: Int = 0,
    @SerializedName("used")
    val used: Int = 0
)

data class ErrorResponse(
    @SerializedName("detail")
    val detail: String
)

