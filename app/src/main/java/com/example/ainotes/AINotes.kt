package com.example.ainotes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.ainotes.data.local.RealmHelper
import com.example.ainotes.utils.BaseUrlManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AINotes : Application() {
    override fun onCreate() {
        super.onCreate()

        // Устанавливаем следование системной теме
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

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