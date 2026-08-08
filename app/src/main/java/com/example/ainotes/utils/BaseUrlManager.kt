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

class BaseUrlManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "secure_prefs"
        private const val KEY_BASE_URL = "key_base_url"
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

    // Используем ConnectionSettingsManager для получения настроек подключения
    private val connectionSettingsManager by lazy { ConnectionSettingsManager(context) }

    fun getBaseUrl(): String {
        // Проверяем режим подключения
        return when {
            // Если используем LM Studio - получаем URL из ConnectionSettingsManager
            connectionSettingsManager.isLMStudioMode() -> {
                val activeUrl = connectionSettingsManager.getActiveUrl()


                // Если используем NGROK - проверяем сохраненный публичный URL
                if (!connectionSettingsManager.isLocalNetworkMode()) {
                    val savedNgrokUrl = sharedPrefs.getString(KEY_BASE_URL, "") ?: ""
                    if (savedNgrokUrl.isNotEmpty()) {

                        return savedNgrokUrl
                    }
                }

                activeUrl
            }
            // Если используем API ключ - возвращаем стандартный URL OpenAI
            else -> {

                "https://api.openai.com"
            }
        }
    }

    fun setBaseUrl(url: String) {
        sharedPrefs.edit().putString(KEY_BASE_URL, url).apply()
    }

    private fun getNgrokApiUrl(): String {
        return connectionSettingsManager.getNgrokApiUrl()
    }

    private fun getNgrokApiKey(): String {
        return connectionSettingsManager.getNgrokApiKey()
    }

    /**
     * Запускает корутину, которая в IO потоке достаёт новый публичный URL из Ngrok
     * и на Main потоке сохраняет его в EncryptedSharedPreferences.
     */
    fun updateBaseUrlFromNgrok() {

        scope.launch {
            val newUrl = fetchNgrokHttpsTunnel()

            if (newUrl != null) {
                // переключаемся на Main для работы с SharedPreferences и UI-лога
                withContext(Dispatchers.Main) {
                    setBaseUrl(newUrl)

                }
            } else {

            }
        }
    }

    /**
     * Выполняет HTTP-запрос к Ngrok API и возвращает первый найденный HTTPS public_url
     */
    private suspend fun fetchNgrokHttpsTunnel(): String? = withContext(Dispatchers.IO) {
        val ngrokApiUrl = getNgrokApiUrl()
        val ngrokApiKey = getNgrokApiKey()

        if (ngrokApiUrl.isEmpty() || ngrokApiKey.isEmpty()) {

            return@withContext null
        }


        var connection: HttpURLConnection? = null
        try {
            val url = URL(ngrokApiUrl)

            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = API_TIMEOUT
                readTimeout = API_TIMEOUT
                setRequestProperty("Authorization", "Bearer $ngrokApiKey")
                setRequestProperty("Ngrok-Version", "2")
            }
            val code = connection.responseCode

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

                    null
                }
                else -> {
                    null
                }
            }
        } catch (e: Exception) {

            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Fetch a fresh HTTPS public_url from ngrok and save it.
     */
    suspend fun refreshPublicUrl(): String? = withContext(Dispatchers.IO) {

        val ngrokApiUrl = getNgrokApiUrl()
        val ngrokApiKey = getNgrokApiKey()

        if (ngrokApiUrl.isEmpty() || ngrokApiKey.isEmpty()) {

            return@withContext null
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(ngrokApiUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = API_TIMEOUT
                readTimeout = API_TIMEOUT
                setRequestProperty("Authorization", "Bearer $ngrokApiKey")
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