package com.example.ainotes.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Менеджер для безопасного хранения настроек подключения
 */
class ConnectionSettingsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "connection_settings_secure_prefs"

        // Ключи для настроек
        private const val KEY_CONNECTION_MODE = "connection_mode" // lm_studio или api_key
        private const val KEY_LM_STUDIO_MODE = "lm_studio_mode" // local или ngrok
        private const val KEY_LOCAL_NETWORK_URL = "local_network_url"
        private const val KEY_NGROK_LOCAL_URL = "ngrok_local_url"
        private const val KEY_NGROK_API_URL = "ngrok_api_url"
        private const val KEY_NGROK_API_KEY = "ngrok_api_key"

        // Значения по умолчанию
        const val CONNECTION_MODE_LM_STUDIO = "lm_studio"
        const val CONNECTION_MODE_API_KEY = "api_key"
        const val LM_STUDIO_MODE_LOCAL = "local"
        const val LM_STUDIO_MODE_NGROK = "ngrok"
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

    // === Режим подключения (LM Studio или API ключ) ===

    fun getConnectionMode(): String {
        return sharedPrefs.getString(KEY_CONNECTION_MODE, CONNECTION_MODE_LM_STUDIO)
            ?: CONNECTION_MODE_LM_STUDIO
    }

    fun setConnectionMode(mode: String) {
        sharedPrefs.edit().putString(KEY_CONNECTION_MODE, mode).apply()
    }

    fun isLMStudioMode(): Boolean {
        return getConnectionMode() == CONNECTION_MODE_LM_STUDIO
    }

    // === Режим LM Studio (локальная сеть или NGROK) ===

    fun getLMStudioMode(): String {
        return sharedPrefs.getString(KEY_LM_STUDIO_MODE, LM_STUDIO_MODE_LOCAL)
            ?: LM_STUDIO_MODE_LOCAL
    }

    fun setLMStudioMode(mode: String) {
        sharedPrefs.edit().putString(KEY_LM_STUDIO_MODE, mode).apply()
    }

    fun isLocalNetworkMode(): Boolean {
        return getLMStudioMode() == LM_STUDIO_MODE_LOCAL
    }

    // === URL локальной сети ===

    fun getLocalNetworkUrl(): String {
        return sharedPrefs.getString(KEY_LOCAL_NETWORK_URL, "http://192.168.1.83:1234")
            ?: "http://192.168.1.83:1234"
    }

    fun setLocalNetworkUrl(url: String) {
        sharedPrefs.edit().putString(KEY_LOCAL_NETWORK_URL, url).apply()
    }

    // === NGROK локальный URL ===

    fun getNgrokLocalUrl(): String {
        return sharedPrefs.getString(KEY_NGROK_LOCAL_URL, "http://192.168.1.83:1234")
            ?: "http://192.168.1.83:1234"
    }

    fun setNgrokLocalUrl(url: String) {
        sharedPrefs.edit().putString(KEY_NGROK_LOCAL_URL, url).apply()
    }

    // === NGROK API URL ===

    fun getNgrokApiUrl(): String {
        return sharedPrefs.getString(KEY_NGROK_API_URL, "https://api.ngrok.com/tunnels")
            ?: "https://api.ngrok.com/tunnels"
    }

    fun setNgrokApiUrl(url: String) {
        sharedPrefs.edit().putString(KEY_NGROK_API_URL, url).apply()
    }

    // === NGROK API KEY ===

    fun getNgrokApiKey(): String {
        return sharedPrefs.getString(KEY_NGROK_API_KEY, "") ?: ""
    }

    fun setNgrokApiKey(key: String) {
        sharedPrefs.edit().putString(KEY_NGROK_API_KEY, key).apply()
    }

    // === Вспомогательные методы ===

    /**
     * Получить активный URL в зависимости от выбранного режима
     */
    fun getActiveUrl(): String {
        val url = when {
            !isLMStudioMode() -> "https://api.openai.com" // Для API ключа используем OpenAI
            isLocalNetworkMode() -> getLocalNetworkUrl()
            else -> getNgrokLocalUrl() // Для NGROK возвращаем локальный URL (публичный URL получается через API)
        }
        android.util.Log.d(
            ">>>ConnectionSettings",
            "🌐 getActiveUrl(): режим=${if (isLMStudioMode()) "LM Studio" else "API Key"}, isLocal=${isLocalNetworkMode()}, URL=$url"
        )
        return url
    }

    /**
     * Очистить все настройки
     */
    fun clearAll() {
        sharedPrefs.edit().clear().apply()
    }
}
