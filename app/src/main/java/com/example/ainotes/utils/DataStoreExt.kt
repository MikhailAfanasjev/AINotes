package com.example.ainotes.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val PREFERENCES_NAME = "settings"

// Этот делегат создаст ровно один DataStore<Preferences> для всего приложения :contentReference[oaicite:0]{index=0}
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)