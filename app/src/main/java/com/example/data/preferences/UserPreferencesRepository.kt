package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY = stringPreferencesKey("preferred_currency")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "dark" // default to stunning dark cyberpunk theme
    }

    val currencyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CURRENCY] ?: "$"
    }

    val hapticsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HAPTICS] ?: true
    }

    suspend fun setThemeMode(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = theme
        }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTICS] = enabled
        }
    }
}
