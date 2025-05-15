package com.example.ainotes.viewModels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val isDarkTheme = dataStore.data
        .map { it[IS_DARK_THEME] ?: false }
        //.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun toggleTheme() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                val current = preferences[IS_DARK_THEME] ?: false
                preferences[IS_DARK_THEME] = !current
            }
        }
    }

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
    }
}