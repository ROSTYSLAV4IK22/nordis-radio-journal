package com.nordisapps.nordisradiojournal

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.nordisapps.nordisradiojournal.loadStations as fetchStationsFromNetwork

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

    private suspend fun ensureMediaControllerReady() {
        if (mediaController == null) {
            initializeMediaController()

            // ждём подключения
            var attempts = 0
            while (mediaController == null && attempts < 20) {
                attempts++
                delay(50) // ждём 50 мс
            }

            if (mediaController == null) {
                Log.e("MediaController", "Controller failed to initialize in time")
            }
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // Пользователь вошел в систему (или сессия восстановлена)
            Log.d("AUTH", "AuthStateListener: User is signed in. UID: ${user.uid}")
            _uiState.update { it.copy(isUserLoggedIn = true) }
            loadUserSpecificData()
        } else {
            // Пользователь вышел
            Log.d("AUTH", "AuthStateListener: User is signed out.")
            _uiState.update {
                it.copy(
                    isUserLoggedIn = false,
                    isUserAdmin = false
                )
            }
        }
    }

    private fun checkAdminStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _uiState.value = _uiState.value.copy(isUserAdmin = false)
            return
        }
        val dbRef = FirebaseDatabase.getInstance().getReference("admins")
        dbRef.get().addOnSuccessListener { snapshot ->
            val adminUids = snapshot.children.map { it.value.toString() }
            val isAdmin = user.uid in adminUids
            _uiState.value = _uiState.value.copy(isUserAdmin = isAdmin)
            if (isAdmin) {
                Log.d("AUTH", "Admin status confirmed for user ${user.uid}")
            }
        }.addOnFailureListener {
            _uiState.value = _uiState.value.copy(isUserAdmin = false)
        }
    }

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
        val tokens = query
            .trim()
            .lowercase()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
        stations.filter { station ->
            val name = station.name?.lowercase().orEmpty()
            val cityName = station.stationCity?.lowercase().orEmpty()
            val mainCity = station.mainCity?.lowercase().orEmpty()
            val countryName = station.country?.lowercase().orEmpty()
            val matchesQuery = tokens.isEmpty() || tokens.all { token ->
                name.contains(token) ||
                        cityName.contains(token) ||
                        mainCity.contains(token) ||
                        countryName.contains(token)
            }
            val matchesCountry = country.isNullOrBlank() || station.country?.equals(
                country,
                ignoreCase = true
            ) == true
            val matchesCity =
                city.isNullOrBlank() || station.mainCity?.equals(city, ignoreCase = true) == true

            matchesQuery && matchesCountry && matchesCity
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var mediaController: MediaController? = null

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        loadStations()

        FirebaseAuth.getInstance().currentUser?.let {
            checkAdminStatus()
        }
    }

    private fun initializeMediaController() {
        viewModelScope.launch {
            try {
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, RadioService::class.java)
                )

                val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
                controllerFuture.addListener({
                    try {
                        mediaController = controllerFuture.get()

                        // Слушаем изменения плеера из сервиса
                        mediaController?.addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                            }

                            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                                val trackInfo = mediaMetadata.title?.toString()
                                    ?: mediaMetadata.artist?.toString()
                                    ?: mediaMetadata.displayTitle?.toString()

                                if (!trackInfo.isNullOrEmpty()) {
                                    _uiState.value =
                                        _uiState.value.copy(currentTrackTitle = trackInfo)
                                }
                            }
                        })

                        Log.d("MediaController", "Successfully connected to RadioService")
                    } catch (e: Exception) {
                        Log.e("MediaController", "Failed to get controller", e)
                    }
                }, MoreExecutors.directExecutor())
            } catch (e: Exception) {
                Log.e("MediaController", "Failed to initialize MediaController", e)
            }
        }
    }

    private fun loadStations() {
        viewModelScope.launch {
            try {
                // Устанавливаем флаг загрузки
                _uiState.update { it.copy(isLoading = true) }

                // Загружаем станции с помощью suspend-функции
                val stationList = fetchStationsFromNetwork()

                // Обновляем состояние со списком станций
                _uiState.update {
                    it.copy(stations = stationList)
                }

                // Загружаем данные, которые зависят от списка станций
                loadRecentlyPlayed()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading stations", e)
                // Здесь можно обработать ошибку, например, показать сообщение пользователю
            } finally {
                // ВАЖНО: Вне зависимости от успеха или ошибки, выключаем индикатор загрузки
                _uiState.update { it.copy(isLoading = false) }
            }
            FirebaseAuth.getInstance().currentUser?.let {
                checkAdminStatus()
            }
        }
    }

    private fun addStationToHistory(station: Station) {
        viewModelScope.launch {
            val currentHistory = _uiState.value.recentlyPlayedStations.toMutableList()

            currentHistory.removeAll { it.id == station.id }
            currentHistory.add(0, station)
            val updatedHistory = currentHistory.take(3)

            _uiState.value = _uiState.value.copy(recentlyPlayedStations = updatedHistory)
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

    private fun loadRecentlyPlayed() {
        viewModelScope.launch {
            val preferences = context.dataStore.data.first()
            val historyString = preferences[RECENTLY_PLAYED_KEY] ?: ""
            if (historyString.isNotEmpty()) {
                val historyIds = historyString.split(",")
                val historyStations = historyIds.mapNotNull { id ->
                    _uiState.value.stations.find { it.id == id }
                }
                _uiState.value = _uiState.value.copy(recentlyPlayedStations = historyStations)
            }
        }
    }

    private fun loadUserSpecificData() {
        loadFavourites()
        checkAdminStatus()
    }

    fun playStation(station: Station) {
        viewModelScope.launch {

            val intent = Intent(context, RadioService::class.java).apply {
                action = RadioService.ACTION_PLAY
                putExtra(RadioService.EXTRA_STREAM_URL, station.stream)
                putExtra(RadioService.EXTRA_STATION_NAME, station.name)
                putExtra(RadioService.EXTRA_STATION_ICON, station.icon)
            }

            context.startService(intent)

            ensureMediaControllerReady()

            _uiState.update {
                it.copy(
                    currentStation = station,
                    currentTrackTitle = null,
                    isPlaying = true
                )
            }

            addStationToHistory(station)
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }

    fun closePlayer() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP
        }
        context.startService(intent)

        _uiState.value = _uiState.value.copy(
            currentStation = null,
            currentTrackTitle = null,
            isPlaying = false,
            currentBitrate = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        mediaController?.release()
        mediaController = null
    }

    fun onUserChanged() {
        loadUserSpecificData()
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
                val preferences = context.dataStore.data.first()
                val favoriteIds = preferences[FAVORITE_STATIONS_KEY] ?: emptySet()
                val favStations = _uiState.value.stations.filter { it.id in favoriteIds }
                _uiState.value = _uiState.value.copy(favouriteStations = favStations)
            }
        }
    }

    fun deleteStation(station: Station, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val stationId = station.id
        if (stationId.isNullOrEmpty()) {
            onFailure(Exception("Cannot delete station with empty ID"))
            return
        }
        val dbRef = FirebaseDatabase.getInstance().getReference("stations")
        dbRef.child(stationId).removeValue()
            .addOnSuccessListener {
                Log.d("ADMIN", "Station deleted from Firebase: $stationId")
                val updateStations = _uiState.value.stations.filterNot { it.id == stationId }
                _uiState.value = _uiState.value.copy(stations = updateStations)
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e("ADMIN", "Failed to delete station: ${error.message}")
                onFailure(error)
            }

    }

    fun saveStation(station: Station, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val dbref = FirebaseDatabase.getInstance().getReference("stations")
        val stationId = station.id

        if (stationId.isNullOrEmpty()) {
            dbref.get().addOnSuccessListener { snapshot ->
                val stations = snapshot.children.mapNotNull { it.getValue(Station::class.java) }
                val maxDisplayId = stations.maxOfOrNull { it.displayId ?: 0 } ?: 0
                val newDisplayId = maxDisplayId + 1

                val newStationRef = dbref.push()
                val newStation = station.copy(
                    id = newStationRef.key,
                    displayId = newDisplayId
                )

                newStationRef.setValue(newStation)
                    .addOnSuccessListener {
                        Log.d("ADMIN", "Station created with displayId $newDisplayId")
                        loadStations()
                        onSuccess()
                    }
            }.addOnFailureListener { error -> onFailure(error) }
        } else {
            dbref.child(stationId).setValue(station)
                .addOnSuccessListener {
                    Log.d("ADMIN", "Station data saved successfully")
                    loadStations()
                    onSuccess()
                }
        }.addOnFailureListener { error -> onFailure(error) }
    }
}
