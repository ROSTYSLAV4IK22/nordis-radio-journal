package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.nordisapps.nordisradiojournal.RadioService
import com.nordisapps.nordisradiojournal.data.Station
import com.nordisapps.nordisradiojournal.data.model.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class PlayerViewModel(
    application: Application,
    private val shared: SharedStateHolder
) : AndroidViewModel(application) {

    val uiStateFlow: StateFlow<UiState> = shared.uiState
    private val context get() = getApplication<Application>().applicationContext
    private var sleepTimerJob: Job? = null
    private var mediaController: MediaController? = null

    private val bitrateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.nordisapps.BITRATE_UPDATE") {
                val bitrate = intent.getIntExtra("bitrate", 0)
                shared.update { it.copy(currentBitrate = bitrate) }
            }
        }
    }

    init {
        LocalBroadcastManager.getInstance(context).registerReceiver(
            bitrateReceiver,
            IntentFilter("com.nordisapps.BITRATE_UPDATE")
        )
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
                                shared.update { it.copy(isPlaying = isPlaying) }
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
                                    shared.update { it.copy(currentTrackTitle = trackInfo) }
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

    fun playStation(station: Station, onStationPlayed: (Station) -> Unit = {}) {
        viewModelScope.launch {

            val intent = Intent(context, RadioService::class.java).apply {
                action = RadioService.ACTION_PLAY
                putExtra(RadioService.EXTRA_STREAM_URL, station.stream)
                putExtra(RadioService.EXTRA_STATION_NAME, station.name)
                putExtra(RadioService.EXTRA_STATION_ICON, station.icon)
            }

            context.startService(intent)

            ensureMediaControllerReady()

            shared.update {
                it.copy(
                    currentStation = station,
                    currentTrackTitle = null,
                    isPlaying = true
                )
            }
            onStationPlayed(station)
        }
    }

    fun togglePlayPause() {
        val state = shared.uiState.value

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

        shared.update {
            it.copy(
                currentStation = null,
                currentTrackTitle = null,
                isPlaying = false,
                currentBitrate = null
            )
        }
    }

    fun stopPlayback() {
        val intent = Intent(context, RadioService::class.java).apply {
            action = RadioService.ACTION_STOP_PLAYBACK
        }
        context.startService(intent)

        shared.update {
            it.copy(
                isPlaying = false,
                currentTrackTitle = null,
                currentBitrate = null
            )
        }
    }

    fun setSleepTimer(minutes: String?) {
        sleepTimerJob?.cancel()
        val mins = minutes?.toIntOrNull()

        if (mins == null) {
            shared.update { it.copy(activeTimerMinutes = null) }
            return
        }

        shared.update {
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
            shared.update {
                it.copy(
                    endTimerTime = null,
                    activeTimerMinutes = null
                )
            }
        }
    }

    override fun onCleared() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(bitrateReceiver)
        mediaController?.release()
        mediaController = null
    }
}