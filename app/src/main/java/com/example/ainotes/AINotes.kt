package com.example.ainotes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.ainotes.data.local.AppDatabase
import com.example.ainotes.utils.BaseUrlManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AINotes : Application() {
    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        AppDatabase.getInstance(this)

        val manager = BaseUrlManager(this)
        manager.updateBaseUrlFromNgrok()
    }
}