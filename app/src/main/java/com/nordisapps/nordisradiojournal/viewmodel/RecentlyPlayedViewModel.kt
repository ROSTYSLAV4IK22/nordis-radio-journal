package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nordisapps.nordisradiojournal.data.RECENTLY_PLAYED_KEY
import com.nordisapps.nordisradiojournal.data.Station
import com.nordisapps.nordisradiojournal.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RecentlyPlayedViewModel(
    application: Application,
    private val shared: SharedStateHolder
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    fun addStationToHistory(station: Station) {
        viewModelScope.launch {
            val currentHistory = shared.uiState.value.recentlyPlayedStations.toMutableList()

            currentHistory.removeAll { it.id == station.id }
            currentHistory.add(0, station)
            val updatedHistory = currentHistory.take(3)

            shared.update { it.copy(recentlyPlayedStations = updatedHistory) }
            saveRecentlyPlayed(updatedHistory)
        }
    }

    private fun saveRecentlyPlayed(history: List<Station>) {
        viewModelScope.launch {
            val historyIds = history.mapNotNull { it.id }
            val historyString = historyIds.joinToString(",")
            context.dataStore.edit { preferences ->
                preferences[RECENTLY_PLAYED_KEY] = historyString
            }
        }
    }

    fun loadRecentlyPlayed() {
        viewModelScope.launch {
            val preferences = context.dataStore.data.first()
            val historyString = preferences[RECENTLY_PLAYED_KEY] ?: ""
            if (historyString.isEmpty()) return@launch

            val historyIds = historyString.split(",")

            val stations = shared.uiState
                .map { it.stations }
                .first { it.isNotEmpty() }

            val historyStations = historyIds.mapNotNull { id ->
                stations.find { it.id == id }
            }
            shared.update { it.copy(recentlyPlayedStations = historyStations) }
        }
    }
}