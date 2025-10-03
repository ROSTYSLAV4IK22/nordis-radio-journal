package com.nordisapps.nordisradiojournal

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.authDataStore by preferencesDataStore(name = "auth")

object AuthManager {
    private val USERNAME_KEY = stringPreferencesKey("username")
    private val PHOTO_KEY = stringPreferencesKey("photo")

    suspend fun saveUser(context: Context, username: String?, photo: String?) {
        context.authDataStore.edit { prefs ->
            prefs[USERNAME_KEY] = username ?: ""
            prefs[PHOTO_KEY] = photo ?: ""
        }
    }

    fun getUser(context: Context): Flow<UserData> {
        return context.authDataStore.data.map { prefs ->
            UserData(
                username = prefs[USERNAME_KEY] ?: "",
                photoUrl = prefs[PHOTO_KEY] ?: ""
            )
        }
    }

    suspend fun clearUser(context: Context) {
        context.authDataStore.edit { it.clear() }
    }
}

data class UserData(
    val username: String,
    val photoUrl: String
)
