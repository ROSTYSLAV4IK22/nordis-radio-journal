package com.nordisapps.nordisradiojournal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nordisapps.nordisradiojournal.ui.components.RadioStationItem
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.util.UnstableApi
import coil.compose.SubcomposeAsyncImage

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    selectedTab: Int
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }

    val stations = uiState.stations
    val favourites = uiState.favouriteStations

    val countries = mapOf(
        "Romania" to listOf("Constanta", "Brasov", "Bucuresti"),
        "Ukraine" to listOf("Odesa", "Mykolayiv", "Kyiv")
    )

    val context = LocalContext.current
    val imageLoader = (context.applicationContext as MyApp).imageLoader

    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var expandedCountry by remember { mutableStateOf(false) }

    var selectedCity by remember { mutableStateOf<String?>(null) }
    var expandedCity by remember { mutableStateOf(false) }

    // 🟢 BackHandler для выхода из поиска
    val focusManager = LocalFocusManager.current
    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty()) {
        focusManager.clearFocus(force = true)
        searchQuery = ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {

            // 🏠 Главная
            0 -> {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.popular_stations),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(stations.take(5)) { station ->
                            Card(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(140.dp),
                                onClick = { viewModel.playStation(station) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    SubcomposeAsyncImage(
                                        model = station.icon ?: "",
                                        imageLoader = imageLoader,
                                        contentDescription = station.name
                                    )
                                    Text(
                                        text = station.name ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🔍 Поиск
            1 -> {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .onFocusChanged { focusState ->
                                isSearchFocused = focusState.isFocused
                            },
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // страна
                        ExposedDropdownMenuBox(
                            expanded = expandedCountry,
                            onExpandedChange = { expandedCountry = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedCountry ?: stringResource(R.string.select_country),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountry)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCountry,
                                onDismissRequest = { expandedCountry = false }
                            ) {
                                countries.keys.forEach { country ->
                                    DropdownMenuItem(
                                        text = { Text(country) },
                                        onClick = {
                                            selectedCountry = country
                                            selectedCity = null
                                            expandedCountry = false
                                        }
                                    )
                                }
                            }
                        }

                        // город
                        if (selectedCountry != null) {
                            ExposedDropdownMenuBox(
                                expanded = expandedCity,
                                onExpandedChange = { expandedCity = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedCity ?: stringResource(R.string.select_city),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCity)
                                    }
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCity,
                                    onDismissRequest = { expandedCity = false }
                                ) {
                                    countries[selectedCountry]!!.forEach { city ->
                                        DropdownMenuItem(
                                            text = { Text(city) },
                                            onClick = {
                                                selectedCity = city
                                                expandedCity = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyColumn {
                        items(stations.filter { station ->
                            val matchesSearch = searchQuery.isEmpty() ||
                                    station.name?.contains(searchQuery, ignoreCase = true) == true
                            val matchedCountry = selectedCountry == null ||
                                    station.country?.equals(selectedCountry, ignoreCase = true) == true
                            val matchesCity = selectedCity == null ||
                                    station.stationCity?.equals(selectedCity, ignoreCase = true) == true
                            matchesSearch && matchedCountry && matchesCity
                        }) { station ->
                            RadioStationItem(
                                icon = station.icon ?: "",
                                name = station.name ?: "",
                                freq = station.freq ?: "",
                                city = station.stationCity ?: "",
                                ps = station.ps ?: "",
                                rt = station.rt ?: "",
                                isFavourite = favourites.any { it.id == station.id },
                                onFavouriteClick = { viewModel.toggleFavourite(station) },
                                onListenClick = { viewModel.playStation(station) }
                            )
                        }
                    }
                }
            }

            // ⭐ Избранное
            2 -> {
                if (favourites.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
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
                } else {
                    LazyColumn {
                        items(favourites) { station ->
                            RadioStationItem(
                                icon = station.icon ?: "",
                                name = station.name ?: "",
                                freq = station.freq ?: "",
                                city = station.stationCity ?: "",
                                ps = station.ps ?: "",
                                rt = station.rt ?: "",
                                isFavourite = true,
                                onFavouriteClick = { viewModel.toggleFavourite(station) },
                                onListenClick = { viewModel.playStation(station) }
                            )
                        }
                    }
                }
            }

            // 🎧 Слушать
            3 -> {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.select_station_to_listen))
                }
            }
        }
    }
}


