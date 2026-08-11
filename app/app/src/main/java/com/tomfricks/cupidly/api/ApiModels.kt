package com.tomfricks.cupidly.api

import com.google.gson.annotations.SerializedName

data class GenerateRepliesRequest(
    @SerializedName("screenshot_base64")
    val screenshotBase64: String,
    @SerializedName("preferences")
    val preferences: PreferencesDto
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
    val suggestions: List<String>
)

data class ErrorResponse(
    @SerializedName("detail")
    val detail: String
)

