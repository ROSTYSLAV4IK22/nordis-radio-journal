package com.nordisapps.nordisradiojournal.ui.helpers

import android.content.Context
import androidx.core.content.edit

object PlayerSettingsManager {
    private const val PREFS_NAME = "player_settings"
    private const val SHOW_SPEED_KEY = "show_speed_kbps"
    private const val SHOW_ICY_METADATA_KEY = "show_icy_metadata"

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isShowSpeedEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(SHOW_SPEED_KEY, true)
    }

    fun setShowSpeedEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(SHOW_SPEED_KEY, enabled) }
    }

    fun isIcyMetadataEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(SHOW_ICY_METADATA_KEY, true)
    }

    fun setIcyMetadataEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit { putBoolean(SHOW_ICY_METADATA_KEY, enabled) }
    }
}