package com.nordisapps.nordisradiojournal

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.metadata.icy.IcyInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.collections.emptyList
import kotlin.collections.emptySet

@Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
@UnstableApi
@OptIn(UnstableApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())

    val uiState: StateFlow<UiState> = _uiState

    private val _languageFlow = MutableStateFlow(LanguageManager.getLanguage(application))
    val languageFlow: StateFlow<String> = _languageFlow

    fun changeLanguage(langCode: String) {
        _languageFlow.value = langCode
    }

    private val context get() = getApplication<Application>().applicationContext

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCountry = MutableStateFlow<String?>(null)
    val selectedCountry: StateFlow<String?> = _selectedCountry

    private val _selectedCity = MutableStateFlow<String?>(null)
    val selectedCity: StateFlow<String?> = _selectedCity

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCountry(country: String?) {
        _selectedCountry.value = country
        _selectedCity.value = null
    }

    fun setSelectedCity(city: String?) {
        _selectedCity.value = city
    }

    val filteredStations = combine(
        _uiState.map { it.stations },
        _searchQuery,
        _selectedCountry,
        _selectedCity
    ) { stations, query, country, city ->
        stations.filter { station ->
            val matchesQuery = query.isBlank() ||
                    station.name?.contains(query, ignoreCase = true) == true ||
                    station.stationCity?.contains(query, ignoreCase = true) == true
            val matchesCountry =
                country.isNullOrBlank() || station.country?.equals (country, ignoreCase = true) == true
            val matchesCity =
                city.isNullOrBlank() || station.mainCity?.equals (city, ignoreCase = true) == true

            matchesQuery && matchesCountry && matchesCity
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            val trackInfo = mediaMetadata.title?.toString()
                ?: mediaMetadata.artist?.toString()
                ?: mediaMetadata.displayTitle?.toString()

            if (!trackInfo.isNullOrEmpty()) {
                _uiState.value = _uiState.value.copy(currentTrackTitle = trackInfo)
            }
        }

        override fun onMetadata(metadata: androidx.media3.common.Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    _uiState.value = _uiState.value.copy(currentTrackTitle = entry.title)
                }
            }
        }
    }

    init {
        exoPlayer.addListener(listener)
        loadStations()
    }

    private fun loadStations() {
        viewModelScope.launch {
            loadStations { loadedStations ->
                _uiState.value = _uiState.value.copy(stations = loadedStations)
                loadFavourites()
            }
        }
    }

    fun playStation(station: Station) {
        val stream = station.stream ?: return
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("NordisRadioJournal/1.0 (Android)")
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(stream))

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.play()

        _uiState.value = _uiState.value.copy(
            currentStation = station,
            isPlaying = true
        )
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun closePlayer() {
        exoPlayer.stop()
        _uiState.value = _uiState.value.copy(
            currentStation = null,
            currentTrackTitle = null,
            isPlaying = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        exoPlayer.removeListener(listener)
        exoPlayer.release()
    }

    fun onUserChanged() {
        loadFavourites()
    }

    fun toggleFavourite(station: Station) {
        val currentFavourites = _uiState.value.favouriteStations.toMutableList()
        if (currentFavourites.any { it.id == station.id }) {
            currentFavourites.removeAll { it.id == station.id }
        } else {
            currentFavourites.add(station)
        }
        _uiState.value = _uiState.value.copy(favouriteStations = currentFavourites)
        saveFavourites()
    }

    private fun saveFavourites() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val favoriteIds = _uiState.value.favouriteStations.mapNotNull { it.id }.toSet()
            if (user != null) {
                FirebaseDatabase.getInstance()
                    .getReference("favorites")
                    .child(user.uid)
                    .setValue(favoriteIds.toList())
            } else {
                context.dataStore.edit { preferences ->
                    preferences[FAVORITE_STATIONS_KEY] = favoriteIds
                }
            }
        }
    }

    private fun loadFavourites() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                FirebaseDatabase.getInstance()
                    .getReference("favorites")
                    .child(user.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val favoriteIds = snapshot.getValue<List<String>>() ?: emptyList()
                        val favStations = _uiState.value.stations.filter { it.id in favoriteIds }
                        _uiState.value = _uiState.value.copy(favouriteStations = favStations)
                    }
            } else {
                context.dataStore.data.collect { preferences ->
                    val favoriteIds = preferences[FAVORITE_STATIONS_KEY] ?: emptySet()
                    val favStations = _uiState.value.stations.filter { it.name in favoriteIds }
                    _uiState.value = _uiState.value.copy(favouriteStations = favStations)
                }
            }
        }
    }
}
