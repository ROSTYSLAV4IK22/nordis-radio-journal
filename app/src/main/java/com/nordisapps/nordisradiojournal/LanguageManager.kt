package com.nordisapps.nordisradiojournal

import android.content.Context
import androidx.core.content.edit
import androidx.core.os.ConfigurationCompat

object LanguageManager {
    private const val PREFS_NAME = "language_settings"
    private const val LANGUAGE_KEY = "selected_language"
    private const val DEFAULT_LANGUAGE = "en"

    val SUPPORTED_LANGUAGES = setOf("en", "ro", "ru", "uk")

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(context: Context): String {
        val saved = getPrefs(context).getString(LANGUAGE_KEY, null)
        if (saved != null) return saved

        val systemLanguage = getSystemLanguageCode(context)
        val resolvedLanguage = if (systemLanguage in SUPPORTED_LANGUAGES) {
            systemLanguage
        } else {
            DEFAULT_LANGUAGE
        }

        saveLanguage(context, resolvedLanguage)
        return resolvedLanguage
    }

    private fun getSystemLanguageCode(context: Context): String {
        val locales = ConfigurationCompat.getLocales(context.resources.configuration)
        return if (!locales.isEmpty) {
            locales[0]?.language ?: DEFAULT_LANGUAGE
        } else {
            DEFAULT_LANGUAGE
        }
    }

    fun saveLanguage(context: Context, langCode: String) {
        getPrefs(context).edit {
            putString(LANGUAGE_KEY, langCode)
        }
    }
}
