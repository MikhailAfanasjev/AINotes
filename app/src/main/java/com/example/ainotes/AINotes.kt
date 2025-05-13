package com.example.ainotes

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.example.ainotes.data.local.RealmHelper
import com.example.ainotes.utils.BaseUrlManager
import com.example.ainotes.utils.dataStore
import com.example.ainotes.viewModels.ThemeViewModel
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class AINotes : Application() {
    override fun onCreate() {
        super.onCreate()

        val initialDark = runBlocking {
            dataStore.data
                .map { prefs -> prefs[ThemeViewModel.IS_DARK_THEME] ?: false }
                .first()
        }
        // 2. Установить режим ночи до того, как любая Activity будет создана
        AppCompatDelegate.setDefaultNightMode(
            if (initialDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Инициализация Realm
        RealmHelper.initRealm(this)

        // Инициализация BaseUrlManager
        val manager = BaseUrlManager(this)

        // Если базовый URL пустой, устанавливаем значение по умолчанию
//        if (manager.getBaseUrl().isBlank()) {
//            manager.setBaseUrl("http://192.168.1.83:1234/")
//        }

        // Обновляем базовый URL из ngrok
        manager.updateBaseUrlFromNgrok()
    }
}