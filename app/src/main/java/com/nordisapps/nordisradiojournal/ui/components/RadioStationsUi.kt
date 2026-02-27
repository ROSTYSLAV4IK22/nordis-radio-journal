@file:Suppress("AssignedValueIsNeverRead")

package com.nordisapps.nordisradiojournal.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.nordisapps.nordisradiojournal.R
import com.nordisapps.nordisradiojournal.ui.theme.LocalImageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RadioStationItem(
    icon: String,
    name: String,
    freq: String,
    city: String,
    location: String?,
    ps: String?,
    rt: String?,
    hasIssues: Boolean?,
    isFavourite: Boolean,
    onFavouriteClick: () -> Unit,
    onListenClick: () -> Unit
) {
    val imageLoader = LocalImageLoader.current
    var expanded by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .bringIntoViewRequester(bringIntoViewRequester),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SubcomposeAsyncImage(
                    model = icon,
                    imageLoader = imageLoader,
                    contentDescription = name,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { showImageDialog = true }
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
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            expanded = !expanded
                            if (expanded) {
                                scope.launch {
                                    delay(180)
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                        .padding(horizontal = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (hasIssues == true) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

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

                IconButton(onClick = { onFavouriteClick() }) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        expanded = !expanded
                        if (expanded) {
                            scope.launch {
                                delay(180)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(expanded) {
                Column {
                    HorizontalDivider()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("${stringResource(R.string.station_location)}: ${location ?: "-"}")
                        Text("PS: ${ps ?: "-"}")
                        Text("RT: ${rt ?: "-"}")
                    }

                    if (hasIssues == true) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.station_stream_issues),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { onListenClick() }
                        )
                        {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(
                                text = stringResource(R.string.btn_listen_online),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImageDialog) {
        EnlargedStationIconDialog(
            iconUrl = icon,
            onDismiss = { showImageDialog = false }
        )
    }
}

@Composable
private fun EnlargedStationIconDialog(
    iconUrl: String,
    onDismiss: () -> Unit
) {
    val imageLoader = LocalImageLoader.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .size(250.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = iconUrl,
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                ) {
                    when (painter.state) {
                        is AsyncImagePainter.State.Loading -> {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }

                        is AsyncImagePainter.State.Error -> {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error loading image",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        else -> {
                            SubcomposeAsyncImageContent()
                        }
                    }
                }
            }
        }
    }
}
