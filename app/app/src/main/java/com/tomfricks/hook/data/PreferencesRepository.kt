package com.tomfricks.hook.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "love_preferences")

class PreferencesRepository(private val context: Context) {

    companion object {
        /**
         * Mirrors the server's lifetime free allowance so the UI can render a
         * sensible "N free left" before the first response comes back.
         */
        const val DEFAULT_FREE_LIMIT = 5

        /**
         * Process-wide cache of the install id, filled the first time it is
         * read. Lets non-suspending callers — chiefly the OkHttp interceptor
         * that stamps `X-App-User-Id` on every request — get the id without
         * waiting on DataStore.
         */
        @Volatile
        var cachedAppUserId: String? = null
            private set
    }

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
        val PROFILE_AGE_RANGE = stringPreferencesKey("profile_age_range")
        val PROFILE_LOOKING_FOR = stringPreferencesKey("profile_looking_for")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val APP_USER_ID = stringPreferencesKey("app_user_id")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val FREE_USED = intPreferencesKey("free_used")
        val FREE_LIMIT = intPreferencesKey("free_limit")
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
            profileAgeRange = preferences[PreferencesKeys.PROFILE_AGE_RANGE] ?: "",
            profileLookingFor = preferences[PreferencesKeys.PROFILE_LOOKING_FOR] ?: "",
            hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false
        )
    }
    
    /** The install id, once it exists. Null until [getOrCreateAppUserId] has run. */
    val appUserIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_USER_ID]
    }

    /** Last known entitlement + free-allowance state, as told to us by the server. */
    val entitlementFlow: Flow<EntitlementState> = context.dataStore.data.map { preferences ->
        EntitlementState(
            isPro = preferences[PreferencesKeys.IS_PRO] ?: false,
            freeUsed = preferences[PreferencesKeys.FREE_USED] ?: 0,
            freeLimit = preferences[PreferencesKeys.FREE_LIMIT] ?: DEFAULT_FREE_LIMIT
        )
    }

    /**
     * The stable per-install id sent as `X-App-User-Id` and used as the
     * RevenueCat app user id. Generated exactly once, on first access, and kept
     * for the life of the install — the server meters the free allowance by it,
     * so it must never rotate.
     */
    suspend fun getOrCreateAppUserId(): String {
        cachedAppUserId?.let { return it }

        // edit() returns the snapshot *after* the transform, so a single
        // read-modify-write both creates and reads the id without a race.
        val updated = context.dataStore.edit { preferences ->
            if (preferences[PreferencesKeys.APP_USER_ID].isNullOrBlank()) {
                preferences[PreferencesKeys.APP_USER_ID] = UUID.randomUUID().toString()
            }
        }
        val id = updated[PreferencesKeys.APP_USER_ID] ?: UUID.randomUUID().toString()
        cachedAppUserId = id
        return id
    }

    suspend fun updateEntitlement(state: EntitlementState) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PRO] = state.isPro
            preferences[PreferencesKeys.FREE_USED] = state.freeUsed
            preferences[PreferencesKeys.FREE_LIMIT] = state.freeLimit
        }
    }

    /** Entitlement-only update, for RevenueCat callbacks that know nothing about usage. */
    suspend fun updateIsPro(isPro: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_PRO] = isPro
        }
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
    
    /**
     * Store the onboarding survey's answers in one write, so a half-saved
     * profile can't survive the app being killed mid-flow.
     *
     * Only non-blank answers are written: a question the flow doesn't ask yet
     * must not wipe an answer given somewhere else.
     */
    suspend fun updateOnboardingProfile(answers: Map<ProfileField, String>) {
        context.dataStore.edit { preferences ->
            answers.forEach { (field, answer) ->
                if (answer.isNotBlank()) preferences[field.key()] = answer
            }
        }
    }

    private fun ProfileField.key() = when (this) {
        ProfileField.GENDER -> PreferencesKeys.PROFILE_GENDER
        ProfileField.SEXUALITY -> PreferencesKeys.PROFILE_SEXUALITY
        ProfileField.AGE_RANGE -> PreferencesKeys.PROFILE_AGE_RANGE
        ProfileField.LOOKING_FOR -> PreferencesKeys.PROFILE_LOOKING_FOR
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
            preferences[PreferencesKeys.PROFILE_AGE_RANGE] = userPreferences.profileAgeRange
            preferences[PreferencesKeys.PROFILE_LOOKING_FOR] = userPreferences.profileLookingFor
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = userPreferences.hasCompletedOnboarding
        }
    }
    
    suspend fun resetAllSettings() {
        context.dataStore.edit { preferences ->
            // "Reset settings" resets *settings*. The install id and the
            // allowance keyed to it deliberately survive: rotating them would
            // hand the user a fresh batch of free generations.
            val appUserId = preferences[PreferencesKeys.APP_USER_ID]
            val isPro = preferences[PreferencesKeys.IS_PRO]
            val freeUsed = preferences[PreferencesKeys.FREE_USED]
            val freeLimit = preferences[PreferencesKeys.FREE_LIMIT]

            preferences.clear()

            if (appUserId != null) preferences[PreferencesKeys.APP_USER_ID] = appUserId
            if (isPro != null) preferences[PreferencesKeys.IS_PRO] = isPro
            if (freeUsed != null) preferences[PreferencesKeys.FREE_USED] = freeUsed
            if (freeLimit != null) preferences[PreferencesKeys.FREE_LIMIT] = freeLimit
        }
    }
}
