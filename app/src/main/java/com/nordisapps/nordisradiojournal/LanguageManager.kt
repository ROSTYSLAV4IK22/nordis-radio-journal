package com.nordisapps.nordisradiojournal

import android.content.Context
import androidx.core.content.edit

object LanguageManager {
    private const val PREFS_NAME = "language_settings"
    private const val LANGUAGE_KEY = "selected_language"
    private const val DEFAULT_LANGUAGE = "en"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun saveLanguage(context: Context, langCode: String) {
        getPrefs(context).edit {
            putString(LANGUAGE_KEY, langCode)
        }
    }
}
