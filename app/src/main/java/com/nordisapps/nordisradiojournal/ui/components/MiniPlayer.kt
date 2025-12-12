@file:Suppress("ConvertTwoComparisonsToRangeCheck")

package com.nordisapps.nordisradiojournal.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.nordisapps.nordisradiojournal.Station
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MiniPlayer(
    station: Station,
    trackTitle: String?,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onClose: () -> Unit,
    onExpandClick: () -> Unit,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier
) {
    var isClicked by remember { mutableStateOf(false) }
    var interactionCounter by remember { mutableIntStateOf(0) }
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionCounter) {
        if (interactionCounter > 0 && isClicked) {
            delay(2000L)
            isClicked = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        isClicked = !isClicked
                        if (isClicked) {
                            interactionCounter++
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubcomposeAsyncImage(
                    model = station.icon,
                    imageLoader = imageLoader,
                    contentDescription = station.name,
                    modifier = Modifier.size(40.dp)
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }

                        is AsyncImagePainter.State.Error -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        else -> SubcomposeAsyncImageContent()
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = station.name ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!trackTitle.isNullOrBlank()) {
                        val density = LocalDensity.current
                        var containerWidth by remember { mutableIntStateOf(0) }
                        var textWidth by remember { mutableIntStateOf(0) }
                        val animationOffset = remember { Animatable(0f) }

                        LaunchedEffect(trackTitle, containerWidth, textWidth) {
                            if (containerWidth > 0 && textWidth > containerWidth) {
                                val scrollDistance = (textWidth - containerWidth).toFloat() + 100f
                                animationOffset.stop()
                                animationOffset.animateTo(
                                    targetValue = scrollDistance,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = (trackTitle.length * 150).coerceAtLeast(
                                                3000
                                            ),
                                            easing = LinearEasing,
                                            delayMillis = 1000
                                        ),
                                        repeatMode = RepeatMode.Restart
                                    )
                                )
                            } else {
                                animationOffset.snapTo(0f)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .padding(top = 2.dp)
                                .clip(RectangleShape)
                                .onGloballyPositioned { coordinates ->
                                    containerWidth = coordinates.size.width
                                }
                        ) {
                            Text(
                                text = trackTitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier
                                    .offset(x = with(density) { -animationOffset.value.toDp() })
                                    .onGloballyPositioned { coordinates ->
                                        textWidth = coordinates.size.width
                                    }
                            )
                        }
                    } else if (isPlaying) {
                        Text(
                            text = station.stationCity ?: "Unknown city",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isClicked) {
                Row {
                    IconButton(onClick = {
                        onPlayPauseClick()
                        interactionCounter++
                    }) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null
                        )
                    }
                }
            } else {
                if (isPlaying) {
                    EqualizerAnimation()
                    IconButton(
                        onClick = onExpandClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Expand Player",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerAnimation() {
    val bars = List(4) { remember { Animatable(10f) } }

    LaunchedEffect(Unit) {
        bars.forEachIndexed { index, bar ->
            launch {
                delay(index * 120L)
                while (true) {
                    val target = (15..35).random().toFloat()
                    bar.animateTo(
                        targetValue = target,
                        animationSpec = tween(
                            durationMillis = 400,
                            easing = LinearEasing
                        )
                    )
                }
            }
        }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        bars.forEach { bar ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(bar.value.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}