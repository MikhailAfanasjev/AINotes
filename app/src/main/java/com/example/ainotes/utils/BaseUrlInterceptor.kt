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



        // Если базовый URL пустой или некорректный, используем исходный запрос
        if (currentBase.isEmpty()) {

            return chain.proceed(req)
        }

        val newBaseUrl = currentBase.toHttpUrlOrNull()
        if (newBaseUrl == null) {

            return chain.proceed(req)  // на случай некорректного URL
        }

        val newUrl = req.url
            .newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()



        val newReq = req.newBuilder()
            .url(newUrl)
            .build()
        return chain.proceed(newReq)
    }
}