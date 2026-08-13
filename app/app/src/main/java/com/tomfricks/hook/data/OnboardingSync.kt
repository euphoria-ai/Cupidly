package com.tomfricks.hook.data

import android.content.Context
import android.util.Log
import com.tomfricks.hook.api.ApiService
import com.tomfricks.hook.api.OnboardingProfileRequest
import kotlinx.coroutines.flow.first

/**
 * Sends the finished onboarding profile to the server, exactly once.
 *
 * "Exactly once" is a local flag, not a server check: the profile is sent when
 * the flow ends, and if that call doesn't land — no signal on the walk home
 * from wherever they downloaded the app — the next launch tries again. The
 * endpoint is an upsert, so a duplicate costs nothing.
 *
 * Nothing here is ever allowed to block or fail the user's own progress. They
 * have finished onboarding; whether our analytics heard about it is our problem.
 */
object OnboardingSync {

    private const val TAG = "OnboardingSync"

    /** Send the profile if onboarding is done and it hasn't gone out yet. */
    suspend fun syncIfNeeded(context: Context, repository: PreferencesRepository) {
        try {
            val preferences = repository.userPreferencesFlow.first()
            if (!preferences.hasCompletedOnboarding) return
            if (repository.onboardingSyncedFlow.first()) return

            val sent = ApiService(context).submitOnboarding(preferences.toOnboardingRequest())
            if (sent) {
                repository.setOnboardingSynced()
            } else {
                Log.d(TAG, "Onboarding profile not sent yet; will retry next launch")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Onboarding sync skipped", e)
        }
    }
}

/**
 * Only the answers onboarding actually asks for. Name, bio and pronouns are
 * edited later in the app and are deliberately not part of this.
 */
private fun UserPreferences.toOnboardingRequest() = OnboardingProfileRequest(
    gender = profileGender.ifBlank { null },
    sexuality = profileSexuality.ifBlank { null },
    ageRange = profileAgeRange.ifBlank { null },
    lookingFor = profileLookingFor.ifBlank { null },
    style = style.name,
    tone = tone.name,
    flirtLevel = flirtLevel.name
)
