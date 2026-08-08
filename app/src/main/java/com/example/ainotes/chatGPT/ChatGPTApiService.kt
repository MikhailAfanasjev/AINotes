package com.example.ainotes.chatGPT

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ChatGPTApiService {
    @POST("v1/chat/completions")
    @Streaming
    suspend fun sendChatMessage(
        @Body request: ChatGPTRequest
    ): Response<ResponseBody>

    @GET("v1/models")
    suspend fun getModels(): Response<ModelsResponse>
}
