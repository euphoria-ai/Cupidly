package com.tomlin7.l0v3.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.tomlin7.l0v3.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface L0V3ApiInterface {
    @POST("/generate-replies")
    suspend fun generateReplies(@Body request: GenerateRepliesRequest): GenerateRepliesResponse
}

class ApiService(private val serverUrl: String = "https://l0v3.onrender.com") {
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(serverUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(L0V3ApiInterface::class.java)
    
    suspend fun generateReplies(
        screenshot: Bitmap,
        preferences: UserPreferences
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            // Convert bitmap to base64
            val base64Image = bitmapToBase64(screenshot)
            
            // Convert UserPreferences to PreferencesDto
            val preferencesDto = PreferencesDto(
                style = preferences.style.name,
                tone = preferences.tone.name,
                flirtLevel = preferences.flirtLevel.name,
                replyLength = preferences.replyLength.name,
                emojiUse = preferences.emojiUse.name,
                profileName = preferences.profileName,
                profileGender = preferences.profileGender,
                profilePronouns = preferences.profilePronouns,
                profileBio = preferences.profileBio
            )

            // print preferencesDto
            Log.d("ApiService", "PreferencesDto: $preferencesDto")
            
            // Create request
            val request = GenerateRepliesRequest(
                screenshotBase64 = base64Image,
                preferences = preferencesDto
            )
            
            // Make API call
            val response = api.generateReplies(request)
            
            if (response.suggestions.isEmpty()) {
                Result.failure(Exception("No suggestions received from server"))
            } else {
                Log.d("ApiService", "Received ${response.suggestions.size} suggestions")
                Result.success(response.suggestions)
            }
            
        } catch (e: Exception) {
            Log.e("ApiService", "Error generating replies", e)
            Result.failure(Exception("Failed to generate replies: ${e.message}"))
        }
    }
    
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress to JPEG with 80% quality to reduce size
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

