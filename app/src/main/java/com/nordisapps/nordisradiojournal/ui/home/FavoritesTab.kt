package com.nordisapps.nordisradiojournal.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nordisapps.nordisradiojournal.R
import com.nordisapps.nordisradiojournal.Station
import com.nordisapps.nordisradiojournal.ui.components.RadioStationItem

@Composable
fun FavoritesTab(
    favourites: List<Station>,
    onFavouriteClick: (Station) -> Unit,
    onStationClick: (Station) -> Unit
) {
    if (favourites.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.StarBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_favorite_stations),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn {
            items(
                count = favourites.size,
                key = { index ->
                    favourites[index].id ?: favourites[index].name ?: index
                }
            ) { index ->
                val station = favourites[index]
                RadioStationItem(
                    icon = station.icon ?: "",
                    name = station.name ?: "",
                    freq = station.freq ?: "",
                    city = station.stationCity ?: "",
                    location = station.location ?: "",
                    ps = station.ps ?: "",
                    rt = station.rt ?: "",
                    hasIssues = station.hasIssues ?: false,
                    isFavourite = true,
                    onFavouriteClick = { onFavouriteClick(station) },
                    onListenClick = { onStationClick(station) }
                )
            }
        }
    }
}