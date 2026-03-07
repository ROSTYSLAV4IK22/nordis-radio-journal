package com.nordisapps.nordisradiojournal

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nordisapps.nordisradiojournal.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings")

val THEME_KEY = intPreferencesKey("theme_mode")

suspend fun saveTheme(context: Context, theme: ThemeMode) {
    context.settingsDataStore.edit { prefs ->
        prefs[THEME_KEY] = theme.ordinal
    }
}

fun getThemeFlow(context: Context): Flow<ThemeMode> {
    return context.settingsDataStore.data.map { prefs ->
        val ordinal = prefs[THEME_KEY] ?: ThemeMode.SYSTEM.ordinal
        ThemeMode.entries[ordinal]
    }
}