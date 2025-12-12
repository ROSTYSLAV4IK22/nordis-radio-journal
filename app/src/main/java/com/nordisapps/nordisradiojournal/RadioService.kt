package com.nordisapps.nordisradiojournal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.core.net.toUri
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo

@OptIn(UnstableApi::class)
class RadioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private lateinit var player: ExoPlayer

    companion object {
        const val CHANNEL_ID = "radio_playback_channel"

        const val ACTION_PLAY = "com.nordisapps.nordisradiojournal.PLAY"
        const val ACTION_PAUSE = "com.nordisapps.nordisradiojournal.PAUSE"
        const val ACTION_STOP = "com.nordisapps.nordisradiojournal.STOP"

        const val EXTRA_STATION_NAME = "station_name"
        const val EXTRA_STATION_ICON = "station_icon"
        const val EXTRA_STREAM_URL = "stream_url"
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("NordisRadioJournal/1.0 (Android)")

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) reconnectAttempts = 0
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                Log.d("RadioService", "Track changed: ${mediaMetadata.title}")
            }

            override fun onMetadata(metadata: Metadata) {
                for (i in 0 until metadata.length()) {
                    val entry = metadata.get(i)
                    if (entry is IcyInfo) {
                        val title = entry.title
                        if (!title.isNullOrEmpty()) {
                            // Обновляем метаданные плеера
                            val currentMetadata = player.mediaMetadata
                            val updatedMetadata = currentMetadata.buildUpon()
                                .setTitle(title)
                                .build()

                            val currentItem = player.currentMediaItem ?: return

                            val newItem = currentItem.buildUpon()
                                .setMediaMetadata(updatedMetadata)
                                .build()

                            player.setMediaItem(newItem, false)

                            Log.d("RadioService", "ICY metadata: $title")
                        }
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("RadioService", "Playback error: ${error.errorCodeName}")

                if (reconnectAttempts >= maxReconnectAttempts) {
                    Log.e("RadioService", "Max reconnect attempts reached")
                    return
                }
                reconnectAttempts++

                player.playWhenReady = false
                player.seekToDefaultPosition()
                player.prepare()

                Log.d("RadioService", "Reconnecting ... attempts $reconnectAttempts")

                player.play()
            }
        })

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                if (bitrateEstimate > 0) {
                    val bitrateKbps = (bitrateEstimate / 1000).toInt()
                    Log.d("RadioService", "Bitrate: $bitrateKbps kbps")
                    // Можно отправить битрейт через BroadcastReceiver в ViewModel
                }
            }
        })

        // Создаём MediaSession
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    val updatedItems = mediaItems.map { item ->
                        item.buildUpon()
                            .setUri(item.requestMetadata.mediaUri)
                            .build()
                    }.toMutableList()
                    return Futures.immediateFuture(updatedItems)
                }
            })
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL) ?: return START_STICKY
                val stationName = intent.getStringExtra(EXTRA_STATION_NAME) ?: "Radio"
                val stationIcon = intent.getStringExtra(EXTRA_STATION_ICON)

                playStream(streamUrl, stationName, stationIcon)
            }

            ACTION_PAUSE -> player.pause()
            ACTION_STOP -> {
                player.stop()
                player.clearMediaItems()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private fun playStream(url: String, name: String, iconUrl: String?) {
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name)
                    .setArtworkUri(iconUrl?.toUri())
                    .build()
            )
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.stop()
        player.clearMediaItems()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        player.stop()
        player.release()
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Radio Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows currently playing radio station"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}