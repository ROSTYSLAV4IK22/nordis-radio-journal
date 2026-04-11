package com.nordisapps.nordisradiojournal

sealed class AdminState {
    object Unknown : AdminState()
    object Admin : AdminState()
    object NotAdmin : AdminState()
}

data class UiState(
    val stations: List<Station> = emptyList(),
    val isLoading: Boolean = true,
    val currentStation: Station? = null,
    val isPlaying: Boolean = false,
    val currentTrackTitle: String? = null,
    val currentBitrate: Int? = null,
    val recentlyPlayedStations: List<Station> = emptyList(),
    val favouriteStations: List<Station> = emptyList(),
    val adminState: AdminState = AdminState.Unknown,
    val isUserLoggedIn: Boolean = false,
    val activeTimerMinutes: String? = null,
    val endTimerTime: String? = null
)