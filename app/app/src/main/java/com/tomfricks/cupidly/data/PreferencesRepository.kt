package com.tomfricks.cupidly.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "love_preferences")

class PreferencesRepository(private val context: Context) {
    
    private object PreferencesKeys {
        val STYLE = stringPreferencesKey("message_style")
        val TONE = stringPreferencesKey("message_tone")
        val FLIRT_LEVEL = stringPreferencesKey("flirt_level")
        val REPLY_LENGTH = stringPreferencesKey("reply_length")
        val EMOJI_USE = stringPreferencesKey("emoji_use")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_GENDER = stringPreferencesKey("profile_gender")
        val PROFILE_SEXUALITY = stringPreferencesKey("profile_sexuality")
        val PROFILE_BIO = stringPreferencesKey("profile_bio")
        val PROFILE_PRONOUNS = stringPreferencesKey("profile_pronouns")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }
    
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            style = preferences[PreferencesKeys.STYLE]?.let { 
                MessageStyle.valueOf(it) 
            } ?: MessageStyle.SENTENCE_CASE,
            tone = preferences[PreferencesKeys.TONE]?.let { 
                MessageTone.valueOf(it) 
            } ?: MessageTone.SMOOTH,
            flirtLevel = preferences[PreferencesKeys.FLIRT_LEVEL]?.let { 
                FlirtLevel.valueOf(it) 
            } ?: FlirtLevel.MEDIUM,
            replyLength = preferences[PreferencesKeys.REPLY_LENGTH]?.let { 
                ReplyLength.valueOf(it) 
            } ?: ReplyLength.NORMAL,
            emojiUse = preferences[PreferencesKeys.EMOJI_USE]?.let { 
                EmojiUse.valueOf(it) 
            } ?: EmojiUse.EXPRESSIVE,
            themeMode = preferences[PreferencesKeys.THEME_MODE]?.let { 
                ThemeMode.valueOf(it) 
            } ?: ThemeMode.SYSTEM,
            profileName = preferences[PreferencesKeys.PROFILE_NAME] ?: "",
            profileGender = preferences[PreferencesKeys.PROFILE_GENDER] ?: "",
            profileSexuality = preferences[PreferencesKeys.PROFILE_SEXUALITY] ?: "",
            profileBio = preferences[PreferencesKeys.PROFILE_BIO] ?: "",
            profilePronouns = preferences[PreferencesKeys.PROFILE_PRONOUNS] ?: "",
            hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
        )
    }
    
    suspend fun updateStyle(style: MessageStyle) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STYLE] = style.name
        }
    }
    
    suspend fun updateTone(tone: MessageTone) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TONE] = tone.name
        }
    }
    
    suspend fun updateFlirtLevel(level: FlirtLevel) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FLIRT_LEVEL] = level.name
        }
    }
    
    suspend fun updateReplyLength(length: ReplyLength) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPLY_LENGTH] = length.name
        }
    }
    
    suspend fun updateEmojiUse(emojiUse: EmojiUse) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMOJI_USE] = emojiUse.name
        }
    }
    
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }
    
    suspend fun updateProfile(
        name: String,
        gender: String,
        sexuality: String,
        bio: String,
        pronouns: String
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PROFILE_NAME] = name
            preferences[PreferencesKeys.PROFILE_GENDER] = gender
            preferences[PreferencesKeys.PROFILE_SEXUALITY] = sexuality
            preferences[PreferencesKeys.PROFILE_BIO] = bio
            preferences[PreferencesKeys.PROFILE_PRONOUNS] = pronouns
        }
    }
    
    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true
        }
    }
    
    suspend fun updateUserPreferences(userPreferences: UserPreferences) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STYLE] = userPreferences.style.name
            preferences[PreferencesKeys.TONE] = userPreferences.tone.name
            preferences[PreferencesKeys.FLIRT_LEVEL] = userPreferences.flirtLevel.name
            preferences[PreferencesKeys.REPLY_LENGTH] = userPreferences.replyLength.name
            preferences[PreferencesKeys.EMOJI_USE] = userPreferences.emojiUse.name
            preferences[PreferencesKeys.THEME_MODE] = userPreferences.themeMode.name
            preferences[PreferencesKeys.PROFILE_NAME] = userPreferences.profileName
            preferences[PreferencesKeys.PROFILE_GENDER] = userPreferences.profileGender
            preferences[PreferencesKeys.PROFILE_SEXUALITY] = userPreferences.profileSexuality
            preferences[PreferencesKeys.PROFILE_BIO] = userPreferences.profileBio
            preferences[PreferencesKeys.PROFILE_PRONOUNS] = userPreferences.profilePronouns
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = userPreferences.hasCompletedOnboarding
        }
    }
    
    suspend fun resetAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
