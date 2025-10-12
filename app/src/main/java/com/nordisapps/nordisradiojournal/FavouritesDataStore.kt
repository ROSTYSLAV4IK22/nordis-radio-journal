package com.nordisapps.nordisradiojournal

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

val FAVORITE_STATIONS_KEY = stringSetPreferencesKey("favorite_stations_ids")
