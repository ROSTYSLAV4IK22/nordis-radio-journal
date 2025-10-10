package com.nordisapps.nordisradiojournal

data class UiState(
    val stations: List<Station> = emptyList(),
    val currentStation: Station? = null,
    val isPlaying: Boolean = false,
    val currentTrackTitle: String? = null,
    val favouriteStations: List<Station> = emptyList()
)