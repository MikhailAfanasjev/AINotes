package com.example.ainotes.chatGPT

import androidx.compose.ui.text.AnnotatedString

data class ChatGPTRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false
)

data class Message(
    val role: String,
    val content: String,
    val isComplete: Boolean = false,
    val stop: List<String>? = null
)

data class ChatGPTResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: Message,
    val finish_reason: String
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)