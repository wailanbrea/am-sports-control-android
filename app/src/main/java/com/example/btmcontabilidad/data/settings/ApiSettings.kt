package com.example.btmcontabilidad.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.btmcontabilidad.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.apiSettingsDataStore by preferencesDataStore(name = "api_settings")

class ApiSettings(private val context: Context) {

    val baseUrl: Flow<String> = context.apiSettingsDataStore.data.map { preferences ->
        when (val storedUrl = preferences[BASE_URL]) {
            null, LEGACY_EMULATOR_URL -> BuildConfig.DEFAULT_API_BASE_URL
            else -> storedUrl
        }
    }

    suspend fun updateBaseUrl(url: String) {
        context.apiSettingsDataStore.edit { preferences ->
            preferences[BASE_URL] = normalizeBaseUrl(url)
        }
    }

    companion object {
        private val BASE_URL = stringPreferencesKey("base_url")
        private const val LEGACY_EMULATOR_URL = "http://10.0.2.2:8000/api/v1/"

        fun normalizeBaseUrl(url: String): String {
            val normalized = url.trim().removeSuffix("/") + "/"
            require(normalized.startsWith("http://") || normalized.startsWith("https://")) {
                "La URL debe iniciar con http:// o https://"
            }
            require(normalized.endsWith("/api/v1/")) {
                "La URL debe terminar en /api/v1/"
            }
            return normalized
        }
    }
}
