package com.nordisapps.nordisradiojournal

data class UiState(
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = true,
    val currentStation: Station? = null,
    val isPlaying: Boolean = false,
    val currentTrackTitle: String? = null,
    val currentBitrate: Int? = null,
    val recentlyPlayedStations: List<Station> = emptyList(),
    val favouriteStations: List<Station> = emptyList(),
    val isUserAdmin: Boolean? = null,
    val isUserLoggedIn: Boolean = false
)