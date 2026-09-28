@file:Suppress("DEPRECATION") // EncryptedSharedPreferences is required for the persisted session token.

package com.example.btmcontabilidad.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = recoverEncryptedPreferences(
        create = ::createPreferences,
        reset = { appContext.deleteSharedPreferences(PREFERENCES_NAME) }
    )

    fun token(): String? = preferences.getString(TOKEN, null)
    fun userName(): String? = preferences.getString(USER_NAME, null)
    fun userEmail(): String? = preferences.getString(USER_EMAIL, null)
    fun userRole(): String = preferences.getString(USER_ROLE, "collector") ?: "collector"

    val isAdmin: Boolean
        get() = userRole().equals("admin", ignoreCase = true)

    val isCollector: Boolean
        get() = !isAdmin

    fun saveToken(token: String) {
        preferences.edit().putString(TOKEN, token).apply()
    }

    fun saveUser(name: String, email: String, role: String = "collector") {
        preferences.edit()
            .putString(USER_NAME, name)
            .putString(USER_EMAIL, email)
            .putString(USER_ROLE, role)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun createPreferences(): SharedPreferences = EncryptedSharedPreferences.create(
        appContext,
        PREFERENCES_NAME,
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private companion object {
        const val PREFERENCES_NAME = "btm_session"
        const val TOKEN = "access_token"
        const val USER_NAME = "user_name"
        const val USER_EMAIL = "user_email"
        const val USER_ROLE = "user_role"
    }
}

internal fun <T> recoverEncryptedPreferences(
    create: () -> T,
    reset: () -> Unit
): T = try {
    create()
} catch (exception: Exception) {
    if (exception !is GeneralSecurityException && exception !is IOException) {
        throw exception
    }

    reset()
    create()
}
