@file:Suppress("SpellCheckingInspection")

package com.nordisapps.nordisradiojournal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import com.nordisapps.nordisradiojournal.Station
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FullPlayer(
    modifier: Modifier = Modifier,
    station: Station,
    trackTitle: String?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    currentBitrate: Int?,
    favouriteStations: List<Station>,
    onToggleFavourite: () -> Unit,
    imageLoader: ImageLoader,
    onDismiss: () -> Unit
) {
    val isFavourite = favouriteStations.any { it.id == station.id }
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)) // Затемнение фона
            .statusBarsPadding() // Отступ от статус-бара
    ) {
        Card(
            modifier = modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        // Позволяем тащить только вниз
                        if (offsetY.value + delta >= 0) {
                            coroutineScope.launch {
                                offsetY.snapTo(offsetY.value + delta)
                            }
                        }
                    },
                    onDragStopped = {
                        coroutineScope.launch {
                            if (offsetY.value > 300) {
                                onDismiss()
                            } else {
                                offsetY.animateTo(0f)
                            }
                        }
                    }
                ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            // ИСПОЛЬЗУЕМ Box, А НЕ Column, В КАЧЕСТВЕ ГЛАВНОГО КОНТЕЙНЕРА
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
            ) {
                // "РУЧКА" - прибита к верху Box'а
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .width(60.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                )

                // ОСНОВНОЙ КОНТЕНТ - находится в Column, который НЕ занимает весь экран
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Отступы, чтобы контент не наезжал на ручку и кнопки
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))
                    // Адаптивная картинка
                    SubcomposeAsyncImage(
                        model = station.icon,
                        imageLoader = imageLoader,
                        contentDescription = station.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Текст
                    Text(
                        text = station.name ?: "Unknown Station",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row {
                        Text(
                            station.freq ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            " • ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            station.stationCity ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!trackTitle.isNullOrBlank()) {
                        Text(
                            text = trackTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    } else if (!station.rt.isNullOrBlank() && station.rt != "-") {
                        Text(
                            text = station.rt,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SignalStrengthIndicator(
                            bitrate = currentBitrate,
                            modifier = Modifier.size(48.dp)
                        )

                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(96.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        IconButton(
                            onClick = onToggleFavourite,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavourite) Icons.Filled.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
