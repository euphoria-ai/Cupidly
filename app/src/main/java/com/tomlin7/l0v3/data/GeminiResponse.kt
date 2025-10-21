package com.tomlin7.l0v3.data

data class GeminiResponse(
    val suggestions: List<String>
)

data class ReplyOption(
    val text: String,
    val index: Int
)
