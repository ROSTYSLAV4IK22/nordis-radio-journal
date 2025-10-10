package com.nordisapps.nordisradiojournal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.rotate
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.ui.res.stringResource
import coil.ImageLoader
import com.nordisapps.nordisradiojournal.R

@Composable
fun RadioStationItem(
    icon: String,
    name: String,
    freq: String,
    city: String,
    location: String?,
    ps: String?,
    rt: String?,
    isFavourite: Boolean,
    imageLoader: ImageLoader,
    onFavouriteClick: () -> Unit,
    onListenClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val onFavouriteHandler = { onFavouriteClick() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubcomposeAsyncImage(
                    model = icon,
                    imageLoader = imageLoader,
                    contentDescription = null,
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
                        name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Row {
                        Text(
                            freq,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            " • ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            city,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onFavouriteHandler)
                )

                Spacer(Modifier.width(8.dp))

                val rotation by animateFloatAsState(if (expanded) 180f else 0f)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(expanded) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Text("${stringResource(R.string.station_location)}: ${location ?: "-"}")
                    Text("PS: ${ps ?: "-"}")
                    Text("RT: ${rt ?: "-"}")

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { onListenClick() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(stringResource(R.string.btn_listen_online))
                    }
                }
            }
        }
    }
}
