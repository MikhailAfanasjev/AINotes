package com.example.ainotes.chatGPT

data class ChatGPTRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false
)

data class Message(
    val role: String,
    val content: String,
    val isComplete: Boolean = false,
    val stop: List<String>? = null,
    val tokenCount: Int = 0,
    val tokensPerSecond: Float = 0f,
    val generationTimeMs: Long = 0L,
    val reasoningContent: String? = null,
    val reasoningDurationSeconds: Float? = null
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

data class ModelsResponse(
    val `object`: String,
    val data: List<ModelData>
)

data class ModelData(
    val id: String,
    val `object`: String,
    val created: Long?,
    val owned_by: String?
)