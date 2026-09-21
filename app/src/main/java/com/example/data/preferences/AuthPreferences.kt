package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AuthProvider
import com.example.data.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notiyape_auth_preferences", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow(loadUser())
    val currentUser: StateFlow<AuthUser> = _currentUser.asStateFlow()

    fun loadUser(): AuthUser {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        if (!isLoggedIn) {
            return AuthUser(isLoggedIn = false)
        }

        val providerStr = prefs.getString(KEY_PROVIDER, AuthProvider.EMAIL.name) ?: AuthProvider.EMAIL.name
        val provider = try {
            AuthProvider.valueOf(providerStr)
        } catch (_: Exception) {
            AuthProvider.EMAIL
        }

        return AuthUser(
            id = prefs.getString(KEY_USER_ID, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            photoUrl = prefs.getString(KEY_PHOTO_URL, "") ?: "",
            whatsapp = prefs.getString(KEY_WHATSAPP, "") ?: "",
            provider = provider,
            isLoggedIn = true,
            token = prefs.getString(KEY_TOKEN, "") ?: "",
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "") ?: "",
            plan = prefs.getString(KEY_PLAN, "FREE") ?: "FREE",
            createdAt = prefs.getLong(KEY_CREATED_AT, System.currentTimeMillis())
        )
    }

    fun saveUser(user: AuthUser) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, user.isLoggedIn)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_PHOTO_URL, user.photoUrl)
            .putString(KEY_WHATSAPP, user.whatsapp)
            .putString(KEY_PROVIDER, user.provider.name)
            .putString(KEY_TOKEN, user.token)
            .putString(KEY_REFRESH_TOKEN, user.refreshToken)
            .putString(KEY_PLAN, user.plan)
            .putLong(KEY_CREATED_AT, user.createdAt)
            .commit()
        _currentUser.value = user
    }

    fun clearSession(keepEmail: Boolean = true) {
        val lastEmail = if (keepEmail) prefs.getString(KEY_EMAIL, "") ?: "" else ""
        prefs.edit().clear().commit()
        if (lastEmail.isNotBlank()) {
            prefs.edit().putString(KEY_EMAIL, lastEmail).commit()
        }
        _currentUser.value = AuthUser(isLoggedIn = false, email = lastEmail)
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "auth_is_logged_in"
        private const val KEY_USER_ID = "auth_user_id"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_DISPLAY_NAME = "auth_display_name"
        private const val KEY_PHOTO_URL = "auth_photo_url"
        private const val KEY_WHATSAPP = "auth_whatsapp"
        private const val KEY_PROVIDER = "auth_provider"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_PLAN = "auth_plan"
        private const val KEY_CREATED_AT = "auth_created_at"

        @Volatile
        private var INSTANCE: AuthPreferences? = null

        fun getInstance(context: Context): AuthPreferences {
            return INSTANCE ?: synchronized(this) {
                val instance = AuthPreferences(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
