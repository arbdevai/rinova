package com.rinova.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rinova_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        private val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        private val KEY_LAST_TARGET_IP = stringPreferencesKey("last_target_ip")
        private val KEY_LAST_TARGET_PORT = stringPreferencesKey("last_target_port")
    }

    val geminiApiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_API_KEY] ?: ""
    }

    val geminiModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_GEMINI_MODEL] ?: "gemini-2.0-flash"
    }

    val lastTargetIp: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_TARGET_IP] ?: "192.168.1."
    }

    val lastTargetPort: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_TARGET_PORT] ?: "5555"
    }

    suspend fun saveGeminiApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_API_KEY] = apiKey.trim()
        }
    }

    suspend fun saveGeminiModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GEMINI_MODEL] = model.trim()
        }
    }

    suspend fun saveLastConnection(ip: String, port: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_TARGET_IP] = ip.trim()
            preferences[KEY_LAST_TARGET_PORT] = port.trim()
        }
    }
}
