package com.example.ainotes.utils

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

class BaseUrlInterceptor(
    private val baseUrlManager: BaseUrlManager
) : Interceptor {
    companion object {
        private const val TAG = ">>>BaseUrlInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val currentBase = baseUrlManager.getBaseUrl()

        Log.d(TAG, "🔗 Исходный URL: ${req.url}")
        Log.d(TAG, "🌐 Базовый URL из настроек: $currentBase")

        // Если базовый URL пустой или некорректный, используем исходный запрос
        if (currentBase.isEmpty()) {
            Log.e(TAG, "❌ Базовый URL пустой, используем исходный запрос")
            return chain.proceed(req)
        }

        val newBaseUrl = currentBase.toHttpUrlOrNull()
        if (newBaseUrl == null) {
            Log.e(TAG, "❌ Некорректный базовый URL: $currentBase")
            return chain.proceed(req)  // на случай некорректного URL
        }

        val newUrl = req.url
            .newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()

        Log.d(TAG, "✅ Новый URL: $newUrl")

        val newReq = req.newBuilder()
            .url(newUrl)
            .build()
        return chain.proceed(newReq)
    }
}