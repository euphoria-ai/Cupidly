package com.tomlin7.cupidly.data

data class GeminiResponse(
    val suggestions: List<String>
)

data class ReplyOption(
    val text: String,
    val index: Int
)
