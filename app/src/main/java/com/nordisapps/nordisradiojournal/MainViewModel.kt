package com.nordisapps.nordisradiojournal

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import com.nordisapps.nordisradiojournal.loadStations as fetchStationsFromNetwork

@Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
@UnstableApi
@OptIn(UnstableApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private companion object {
        const val ENABLE_ANNOUNCEMENTS = false
    }

    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState
    private val _languageFlow = MutableStateFlow(LanguageManager.getLanguage(application))
    val languageFlow: StateFlow<String> = _languageFlow

    fun changeLanguage(langCode: String) {
        _languageFlow.value = langCode
    }

    private val context get() = getApplication<Application>().applicationContext
    private var sleepTimerJob: Job? = null
    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters
    private val firestore = FirebaseFirestore.getInstance()

    var announcement by mutableStateOf<Announcement?>(null)
        private set

    var christmasDeco by mutableStateOf<Announcement?>(null)
        private set

    val isChristmas = mutableStateOf(checkChristmas())

    private fun checkChristmas(): Boolean {
        val today = LocalDate.now()

        val start = if (today.month >= Month.DECEMBER) {
            LocalDate.of(today.year, Month.DECEMBER, 1)
        } else {
            LocalDate.of(today.year - 1, Month.DECEMBER, 1)
        }
        val end = start.plusMonths(2)

        return !today.isBefore(start) && today.isBefore(end)
    }

    /**
     * Announcement system (disabled for now)
     *
     * Planned usage:
     * - Time-limited announcements
     * - Priority-based selection
     * - Seasonal / admin controlled messages
     *
     * TODO: Enable after UI/UX decision
     */
    private fun loadAnnouncement() {
        firestore.collection("announcements")
            .whereEqualTo("enabled", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val today = LocalDate.now()

                val activeAnnouncement = snapshot.documents
                    .mapNotNull { it.toObject(Announcement::class.java) }
                    .filter { isInDateRange(it, today) }
                    .maxByOrNull { it.priority }

                announcement = activeAnnouncement
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
            .addOnFailureListener {
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
    }

    private fun isInDateRange(a: Announcement, today: LocalDate): Boolean {
        val start = LocalDate.parse(a.startDate)
        val end = LocalDate.parse(a.endDate)
        return !today.isBefore(start) && today.isBefore(end)
    }

    /** fun dismissAnnouncement() {
        announcement = null
    } */

    private fun loadChristmasDeco() {
        firestore.collection("announcements")
            .whereEqualTo("enabled", true)
            .whereEqualTo("type", "christmas")
            .get()
            .addOnSuccessListener { snapshot ->
                val today = LocalDate.now()

                christmasDeco = snapshot.documents
                    .mapNotNull { it.toObject(Announcement::class.java) }
                    .firstOrNull {
                        !today.isBefore(LocalDate.parse(it.startDate)) && today.isBefore(
                            LocalDate.parse(it.endDate)
                        )
                    }
            }
    }

    private val bitrateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.nordisapps.BITRATE_UPDATE") {
                val bitrate = intent.getIntExtra("bitrate", 0)

                _uiState.update {
                    it.copy(currentBitrate = bitrate)
                }
            }
        }
    }

    private suspend fun ensureMediaControllerReady() {
        if (mediaController == null) {
            initializeMediaController()

            var attempts = 0
            while (mediaController == null && attempts < 20) {
                attempts++
                delay(50.milliseconds)
            }

            if (mediaController == null) {
                Log.e("MediaController", "Controller failed to initialize in time")
            }
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            _uiState.update {
                it.copy(
                    isUserLoggedIn = true,
                    adminState = AdminState.Unknown
                )
            }
            checkAdminStatus(user.uid)
            mergeFavouritesOnLogin(user.uid)
        } else {
            _uiState.update {
                it.copy(
                    isUserLoggedIn = false,
                    adminState = AdminState.NotAdmin
                )
            }
        }
    }

    private fun checkAdminStatus(uid: String) {
        FirebaseDatabase.getInstance()
            .getReference("admins")
            .get()
            .addOnSuccessListener { snapshot ->
                val isAdmin = snapshot.children.any { it.value == uid }
                _uiState.update {
                    it.copy(
                        adminState = if (isAdmin)
                            AdminState.Admin
                        else
                            AdminState.NotAdmin
                    )
                }
            }
            .addOnFailureListener {
                _uiState.update {
                    it.copy(adminState = AdminState.NotAdmin)
                }
            }
    }

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun setSelectedCountry(country: String?) {
        _filters.update { it.copy(country = country, city = null, coverage = emptySet()) }
    }

    fun setSelectedCity(city: String?) {
        _filters.update {
            it.copy(
                city = city,
                coverage = if (city != null) setOf(city) else emptySet()
            )
        }
    }

    fun setSelectedCoverage(coverage: Set<String>) {
        _filters.update {
            it.copy(
                coverage = coverage,
                city = if (coverage.size == 1) coverage.first() else null
            )
        }
    }

    val filteredStations = combine(
        _uiState.map { it.stations },
        _filters
    ) { stations, filters ->
        val tokens = filters.query
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

            val matchesCountry = filters.country.isNullOrBlank() || station.country?.equals(
                filters.country,
                ignoreCase = true
            ) == true

            val matchesCity = if (filters.coverage.isNotEmpty()) {
                true
            } else {
                filters.city.isNullOrBlank() || station.mainCity?.equals(filters.city, ignoreCase = true) == true
            }

            val matchesCoverage = filters.coverage.isEmpty() || station.coverage?.any { it in filters.coverage } == true

            matchesQuery && matchesCountry && matchesCity && matchesCoverage
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var mediaController: MediaController? = null

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
        loadStations()
        if (ENABLE_ANNOUNCEMENTS) {
            loadAnnouncement()
        }
        loadChristmasDeco()
        LocalBroadcastManager.getInstance(context).registerReceiver(
            bitrateReceiver,
            IntentFilter("com.nordisapps.BITRATE_UPDATE")
        )
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

                        mediaController?.addListener(object : Player.Listener {
                            override fun onIsPlayingChanged(isPlaying: Boolean) {
                                _uiState.value = _uiState.value.copy(isPlaying = isPlaying)
                            }

                            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                                val title = mediaMetadata.title?.toString()
                                val artist = mediaMetadata.artist?.toString()

                                val trackInfo = when {
                                    !title.isNullOrBlank() && !artist.isNullOrBlank() -> "$artist - $title"
                                    !title.isNullOrBlank() -> title
                                    !artist.isNullOrBlank() -> artist
                                    else -> null
                                }

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
                _uiState.update { it.copy(isLoading = true) }

                val stationList = fetchStationsFromNetwork()

                _uiState.update {
                    it.copy(stations = stationList)
                }

                loadRecentlyPlayed()
                loadFavourites()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error loading stations", e)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
        val state = _uiState.value

        if (state.isPlaying) {
            stopPlayback()
        } else {
            state.currentStation?.let {
                playStation(it)
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

    fun stopPlayback() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP_PLAYBACK
        }
        context.startService(intent)

        _uiState.update {
            it.copy(
                isPlaying = false,
                currentTrackTitle = null,
                currentBitrate = null
            )
        }
    }

    override fun onCleared() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(bitrateReceiver)
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        mediaController?.release()
        mediaController = null
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
                        val favoriteIds =
                            snapshot.children.mapNotNull { it.getValue(String::class.java) }
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

    private fun mergeFavouritesOnLogin(uid: String) {
        viewModelScope.launch {
            val preferences = context.dataStore.data.first()
            val localIds = preferences[FAVORITE_STATIONS_KEY] ?: emptySet()

            if (localIds.isEmpty()) {
                loadFavourites()
                return@launch
            }

            val ref = FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(uid)

            ref.get().addOnSuccessListener { snapshot ->
                val firebaseIds = snapshot.children
                    .mapNotNull { it.getValue(String::class.java) }
                    .toSet()

                val mergedIds = (localIds + firebaseIds).toList()

                ref.setValue(mergedIds).addOnSuccessListener {
                    viewModelScope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[FAVORITE_STATIONS_KEY] = emptySet()
                        }
                    }
                    val favStations = _uiState.value.stations.filter { it.id in mergedIds }
                    _uiState.update { it.copy(favouriteStations = favStations) }
                }
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

    fun setSleepTimer(minutes: String?) {
        sleepTimerJob?.cancel()
        val mins = minutes?.toIntOrNull()

        if (mins == null) {
            _uiState.update { it.copy(activeTimerMinutes = null) }
            return
        }

        _uiState.update {
            it.copy(
                activeTimerMinutes = minutes,
                endTimerTime = LocalTime.now().plusMinutes(minutes.toLong()).format(
                    DateTimeFormatter.ofPattern("HH:mm")
                )
            )
        }

        sleepTimerJob = viewModelScope.launch {
            delay(mins.minutes)
            stopPlayback()
            _uiState.update {
                it.copy(
                    endTimerTime = null,
                    activeTimerMinutes = null
                )
            }
        }
    }
}
