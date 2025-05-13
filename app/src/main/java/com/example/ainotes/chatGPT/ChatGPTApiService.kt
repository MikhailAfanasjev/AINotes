package com.example.ainotes.chatGPT

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Streaming

interface ChatGPTApiService {
    @POST("v1/chat/completions")
    @Streaming
    fun sendChatMessageCall(
        @Body request: ChatGPTRequest
    ): Call<ResponseBody>
}