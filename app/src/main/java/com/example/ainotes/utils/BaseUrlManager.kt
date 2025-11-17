package com.example.ainotes.utils

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class BaseUrlManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_BASE_URL = "key_base_url"
        private const val DEFAULT_URL = "https://9105-84-17-46-88.ngrok-free.app"

        private const val NGROK_API_URL = "https://api.ngrok.com/tunnels"
        private const val API_KEY = "2vwuX6rCb0W5FrInoQ9yPPCr7wt_3qvbbxb9T4kLyjtwDRNoL"
        private const val API_TIMEOUT = 15_000

        private const val TAG = ">>>BaseUrlManager"
    }

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Скоуп для фоновых корутин; SupervisorJob чтобы одна ошибка не отменяла другие
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun getBaseUrl(): String {
        val baseUrl = sharedPrefs.getString(KEY_BASE_URL, DEFAULT_URL) ?: DEFAULT_URL
        return baseUrl
    }

    private fun setBaseUrl(url: String) {
        sharedPrefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    /**
     * Запускает корутину, которая в IO потоке достаёт новый публичный URL из Ngrok
     * и на Main потоке сохраняет его в EncryptedSharedPreferences.
     */
    fun updateBaseUrlFromNgrok() {
        Log.d(TAG, "🚀 updateBaseUrlFromNgrok(): старт корутины для запроса Ngrok URL")
        scope.launch {
            val newUrl = fetchNgrokHttpsTunnel()
            Log.d(TAG, "🔄 fetchNgrokHttpsTunnel() вернул: $newUrl")
            if (newUrl != null) {
                // переключаемся на Main для работы с SharedPreferences и UI-лога
                withContext(Dispatchers.Main) {
                    setBaseUrl(newUrl)
                    Log.d(TAG, "✅ setBaseUrl(): сохранён новый URL -> $newUrl")
                }
            } else {
                Log.w(TAG, "⚠️ fetchNgrokHttpsTunnel() вернул null, URL не обновлён")
            }
        }
    }

    /**
     * Выполняет HTTP-запрос к Ngrok API и возвращает первый найденный HTTPS public_url
     */
    private suspend fun fetchNgrokHttpsTunnel(): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "🌐 fetchNgrokHttpsTunnel(): делаем GET $NGROK_API_URL")
        var connection: HttpURLConnection? = null
        try {
            val url = URL(NGROK_API_URL)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = API_TIMEOUT
                readTimeout = API_TIMEOUT
                setRequestProperty("Authorization", "Bearer $API_KEY")
                setRequestProperty("Ngrok-Version", "2")
            }
            val code = connection.responseCode
            Log.d(TAG, "📶 Response code: $code")
            return@withContext when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                        parseNgrokResponse(reader.readText())
                    }
                }
                HttpURLConnection.HTTP_UNAUTHORIZED -> {
                    null
                }
                HttpURLConnection.HTTP_FORBIDDEN -> {
                    Log.e(TAG, "❌ Unauthorized/Forbidden при запросе к Ngrok API")
                    null
                }
                else -> {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "🔥 Ошибка при запросе Ngrok API", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetch a fresh HTTPS public_url from ngrok and save it.
     */
    suspend fun refreshPublicUrl(): String? = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔄 refreshPublicUrl(): попытка обновить URL из Ngrok API")
        var connection: HttpURLConnection? = null
        try {
            val url = URL(NGROK_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = API_TIMEOUT
                readTimeout = API_TIMEOUT
                setRequestProperty("Authorization", "Bearer $API_KEY")
                setRequestProperty("Ngrok-Version", "2")
            }
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val newUrl = parseNgrokResponse(body)
                if (!newUrl.isNullOrBlank()) {
                    setBaseUrl(newUrl)
                }
                return@withContext newUrl
            }
        } catch (_: Exception) {
        } finally {
            connection?.disconnect()
        }
        return@withContext null
    }


    private fun parseNgrokResponse(response: String): String? {
        return try {
            val json = JSONObject(response)
            val tunnels = json.getJSONArray("tunnels")
            for (i in 0 until tunnels.length()) {
                val tunnel = tunnels.getJSONObject(i)
                if (tunnel.getString("proto") == "https") {
                    val publicUrl = tunnel.getString("public_url")
                    return publicUrl
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}