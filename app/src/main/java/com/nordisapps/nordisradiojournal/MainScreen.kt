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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.util.UnstableApi
import coil.compose.SubcomposeAsyncImage

data class LocationItem(val key: String, val displayName: String)

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    selectedTab: Int
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }

    val stations = uiState.stations
    val favourites = uiState.favouriteStations
    val filteredStations by viewModel.filteredStations.collectAsState()

    val keyRomania = stringResource(R.string.key_country_romania)
    val keyUkraine = stringResource(R.string.key_country_ukraine)

    val displayRomania = stringResource(R.string.country_romania)
    val displayUkraine = stringResource(R.string.country_ukraine)

    val countries = remember(displayRomania, displayUkraine) {
        listOf(
            LocationItem(keyRomania, displayRomania),
            LocationItem(keyUkraine, displayUkraine)
        )
    }

    val keyConstanta = stringResource(R.string.key_city_constanta)
    val displayConstanta = stringResource(R.string.city_constanta)
    val keyBrasov = stringResource(R.string.key_city_brasov)
    val displayBrasov = stringResource(R.string.city_brasov)
    val keyBucharest = stringResource(R.string.key_city_bucharest)
    val displayBucharest = stringResource(R.string.city_bucharest)

    val keyOdessa = stringResource(R.string.key_city_odessa)
    val displayOdessa = stringResource(R.string.city_odessa)
    val keyKiev = stringResource(R.string.key_city_kiev)
    val displayKiev = stringResource(R.string.city_kiev)
    val keyNikolaev = stringResource(R.string.key_city_nikolaev)
    val displayNikolaev = stringResource(R.string.city_nikolaev)

    val citiesByCountry = remember(
        keyRomania, keyUkraine, keyConstanta, displayConstanta, keyBrasov, displayBrasov,
        keyBucharest, displayBucharest, keyOdessa, displayOdessa, keyKiev, displayKiev,
        keyNikolaev, displayNikolaev
    ) {
        mapOf(
            keyRomania to listOf(
                LocationItem(keyConstanta, displayConstanta),
                LocationItem(keyBrasov, displayBrasov),
                LocationItem(keyBucharest, displayBucharest)
            ),
            keyUkraine to listOf(
                LocationItem(keyOdessa, displayOdessa),
                LocationItem(keyKiev, displayKiev),
                LocationItem(keyNikolaev, displayNikolaev)
            )
        )
    }

    val context = LocalContext.current
    val imageLoader = (context.applicationContext as MyApp).imageLoader

    val selectedCountryKey by viewModel.selectedCountry.collectAsState()
    var expandedCountry by remember { mutableStateOf(false) }

    val selectedCityKey by viewModel.selectedCity.collectAsState()
    var expandedCity by remember { mutableStateOf(false) }

    // 🟢 BackHandler для выхода из поиска
    val focusManager = LocalFocusManager.current
    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty()) {
        focusManager.clearFocus(force = true)
        viewModel.setSearchQuery("")
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
                                        modifier = Modifier
                                            .size(64.dp)
                                            .weight(1f, fill = false),
                                        contentScale = ContentScale.Fit,
                                        model = station.icon ?: "",
                                        imageLoader = imageLoader,
                                        contentDescription = station.name
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = station.name ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
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
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .onFocusChanged { focusState ->
                                isSearchFocused = focusState.isFocused
                            },
                        shape = RoundedCornerShape(50.dp),
                        placeholder = { Text(stringResource(R.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, null)
                                }
                            }
                        },
                        singleLine = true
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
                                value = countries.find { it.key == selectedCountryKey }?.displayName
                                    ?: stringResource(R.string.select_country),
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountry)
                                },
                                singleLine = true
                            )
                            ExposedDropdownMenu(
                                expanded = expandedCountry,
                                onDismissRequest = { expandedCountry = false }
                            ) {
                                countries.forEach { countryItem ->
                                    DropdownMenuItem(
                                        text = { Text(countryItem.displayName) }, // Показываем переведенное имя
                                        onClick = {
                                            viewModel.setSelectedCountry(countryItem.key) // Сохраняем непереводимый ключ
                                            viewModel.setSelectedCity(null)
                                            expandedCountry = false
                                        }
                                    )
                                }
                            }
                        }

                        // город
                        if (selectedCountryKey != null) {
                            ExposedDropdownMenuBox(
                                expanded = expandedCity,
                                onExpandedChange = { expandedCity = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                val cities = citiesByCountry[selectedCountryKey] ?: emptyList()
                                OutlinedTextField(
                                    value = cities.find { it.key == selectedCityKey }?.displayName
                                        ?: stringResource(R.string.select_city),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCity)
                                    },
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedCity,
                                    onDismissRequest = { expandedCity = false }
                                ) {
                                    cities.forEach { cityItem ->
                                        DropdownMenuItem(
                                            text = { Text(cityItem.displayName) }, // Показываем переведенное имя
                                            onClick = {
                                                viewModel.setSelectedCity(cityItem.key) // Сохраняем непереводимый ключ
                                                expandedCity = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val isFilterActive =
                        searchQuery.isNotEmpty() || selectedCountryKey != null || selectedCityKey != null

                    if (isFilterActive) {
                        LazyColumn {
                            items(filteredStations, key = { it.id ?: it.name ?: "" }) { station ->
                                RadioStationItem(
                                    icon = station.icon ?: "",
                                    name = station.name ?: "",
                                    freq = station.freq ?: "",
                                    city = station.stationCity ?: "",
                                    location = station.location ?: "",
                                    ps = station.ps ?: "",
                                    rt = station.rt ?: "",
                                    isFavourite = favourites.any { it.id == station.id },
                                    imageLoader = imageLoader,
                                    onFavouriteClick = { viewModel.toggleFavourite(station) },
                                    onListenClick = { viewModel.playStation(station) }
                                )
                            }
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
                                isFavourite = true,
                                imageLoader = imageLoader,
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
