package com.tomlin7.l0v3.api

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.tomlin7.l0v3.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class GeminiService(private val apiKey: String) {
    
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.0-flash-exp",
            apiKey = apiKey
        )
    }
    
    suspend fun generateReplies(
        screenshot: Bitmap,
        preferences: UserPreferences
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildPrompt(preferences)
            
            val inputContent = content {
                image(screenshot)
                text(prompt)
            }
            
            val response = generativeModel.generateContent(inputContent)
            val responseText = response.text ?: throw Exception("Empty response from Gemini")
            
            Log.d("GeminiService", "Raw response: $responseText")
            
            // Parse the response to extract the 3 suggestions
            val suggestions = parseResponse(responseText)
            
            if (suggestions.isEmpty()) {
                throw Exception("No valid suggestions found in response")
            }
            
            Result.success(suggestions)
        } catch (e: Exception) {
            Log.e("GeminiService", "Error generating replies", e)
            Result.failure(e)
        }
    }
    
    private fun buildPrompt(preferences: UserPreferences): String {
        return buildString {
            appendLine("You are an AI texting assistant helping to generate replies for chat conversations.")
            appendLine()
            appendLine("Analyze the chat screenshot and generate EXACTLY 3 different reply options.")
            appendLine()
            appendLine("USER PREFERENCES:")
            appendLine("- Style: ${preferences.style.displayName}")
            appendLine("- Tone: ${preferences.tone.displayName}")
            appendLine("- Flirt Level: ${preferences.flirtLevel.displayName}")
            appendLine("- Reply Length: ${preferences.replyLength.displayName}")
            appendLine("- Emoji Use: ${preferences.emojiUse.displayName}")
            
            if (preferences.profileName.isNotEmpty()) {
                appendLine("- Name: ${preferences.profileName}")
            }
            if (preferences.profileGender.isNotEmpty()) {
                appendLine("- Gender: ${preferences.profileGender}")
            }
            if (preferences.profilePronouns.isNotEmpty()) {
                appendLine("- Pronouns: ${preferences.profilePronouns}")
            }
            if (preferences.profileBio.isNotEmpty()) {
                appendLine("- About: ${preferences.profileBio}")
            }
            
            appendLine()
            appendLine("IMPORTANT INSTRUCTIONS:")
            appendLine("1. Generate EXACTLY 3 distinct reply options")
            appendLine("2. Make each reply unique in approach (e.g., funny, smooth, engaging)")
            appendLine("3. Match the conversation context and tone")
            appendLine("4. Keep replies natural and conversational")
            appendLine("5. Apply the user's style preferences to each reply")
            appendLine()
            appendLine("OUTPUT FORMAT:")
            appendLine("Reply 1: [first reply text]")
            appendLine("Reply 2: [second reply text]")
            appendLine("Reply 3: [third reply text]")
            appendLine()
            appendLine("Generate the replies now:")
        }
    }
    
    private fun parseResponse(responseText: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        // Try to parse structured format first
        val replyPattern = Regex("""Reply\s*\d+:\s*(.+?)(?=Reply\s*\d+:|$)""", RegexOption.DOT_MATCHES_ALL)
        val matches = replyPattern.findAll(responseText)
        
        for (match in matches) {
            val reply = match.groupValues[1].trim()
            if (reply.isNotEmpty()) {
                suggestions.add(reply)
            }
        }
        
        // If structured parsing failed, try line-by-line
        if (suggestions.isEmpty()) {
            val lines = responseText.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("Reply") && !it.contains("USER PREFERENCES") }
            
            suggestions.addAll(lines.take(3))
        }
        
        // If still empty, try JSON parsing
        if (suggestions.isEmpty()) {
            try {
                val jsonArray = JSONArray(responseText)
                for (i in 0 until minOf(3, jsonArray.length())) {
                    suggestions.add(jsonArray.getString(i))
                }
            } catch (e: Exception) {
                Log.w("GeminiService", "Failed to parse as JSON", e)
            }
        }
        
        // Fallback: split by newlines and take first 3 non-empty lines
        if (suggestions.isEmpty()) {
            suggestions.addAll(
                responseText.lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)
            )
        }
        
        return suggestions.take(3)
    }
}
