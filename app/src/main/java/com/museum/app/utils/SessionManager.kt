package com.museum.app.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

// Храним сессию (кто вошёл) и настройку темы в SharedPreferences,
// чтобы после перезапуска приложения не надо было заново логиниться
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("museum_session", Context.MODE_PRIVATE)

    fun saveUser(id: Long, role: String) {
        prefs.edit()
            .putLong("user_id", id)
            .putString("role", role)
            .apply()
    }

    fun getUserId(): Long = prefs.getLong("user_id", -1)

    fun isAdmin(): Boolean = prefs.getString("role", "") == "admin"

    fun isLoggedIn(): Boolean = getUserId() != -1L

    fun logout() {
        prefs.edit()
            .remove("user_id")
            .remove("role")
            .apply()
    }

    fun isDarkTheme(): Boolean = prefs.getBoolean("dark_theme", false)

    fun setDarkTheme(dark: Boolean) {
        prefs.edit().putBoolean("dark_theme", dark).apply()
        applyTheme()
    }

    // включает светлую или тёмную тему для всего приложения
    fun applyTheme() {
        if (isDarkTheme()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
