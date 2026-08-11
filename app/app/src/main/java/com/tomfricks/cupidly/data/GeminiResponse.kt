package com.tomfricks.cupidly.data

data class GeminiResponse(
    val suggestions: List<String>
)

data class ReplyOption(
    val text: String,
    val index: Int
)
