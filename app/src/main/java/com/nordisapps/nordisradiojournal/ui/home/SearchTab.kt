@file:Suppress("AssignedValueIsNeverRead")

package com.nordisapps.nordisradiojournal.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nordisapps.nordisradiojournal.R
import com.nordisapps.nordisradiojournal.Station
import com.nordisapps.nordisradiojournal.ui.components.RadioStationItem

data class LocationItem(val key: String, val displayName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(
    searchQuery: String,
    selectedCountryKey: String?,
    selectedCityKey: String?,
    selectedCoverageKeys: Set<String>,
    filteredStations: List<Station>,
    favourites: List<Station>,
    onSearchQueryChange: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onCitySelected: (String?) -> Unit,
    onCoverageSelected: (Set<String>) -> Unit,
    onFavouriteClick: (Station) -> Unit,
    onListenClick: (Station) -> Unit
) {
    var isSearchFocused by rememberSaveable { mutableStateOf(false) }

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

    var showCountrySheet by remember { mutableStateOf(false) }
    var showCitySheet by remember { mutableStateOf(false) }
    var showCoverageSheet by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    BackHandler(enabled = isSearchFocused || searchQuery.isNotEmpty()) {
        focusManager.clearFocus(force = true)
        onSearchQueryChange("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 56.dp)
                    .onFocusChanged { focusState ->
                        isSearchFocused = focusState.isFocused
                    },
                shape = RoundedCornerShape(50.dp),
                placeholder = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            stringResource(R.string.search_placeholder),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                },
                singleLine = true
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = selectedCountryKey != null,
                    onClick = { showCountrySheet = true },
                    label = {
                        Text(
                            text = countries.find { it.key == selectedCountryKey }?.displayName
                                ?: stringResource(R.string.select_country)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                )

                if (showCountrySheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showCountrySheet = false }
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            items(countries) { countryItem ->
                                Text(
                                    text = countryItem.displayName,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onCountrySelected(countryItem.key)
                                            onCitySelected(null)
                                            showCountrySheet = false
                                        }
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                                )
                            }
                        }
                    }
                }

                if (selectedCountryKey != null) {
                    val cities = citiesByCountry[selectedCountryKey] ?: emptyList()

                    FilterChip(
                        selected = selectedCityKey != null,
                        onClick = { showCitySheet = true },
                        label = {
                            Text(
                                text = cities.find { it.key == selectedCityKey }?.displayName
                                    ?: stringResource(R.string.select_city)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                    )

                    if (showCitySheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showCitySheet = false }
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                items(cities) { cityItem ->
                                    Text(
                                        text = cityItem.displayName,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                onCitySelected(cityItem.key)
                                                showCitySheet = false
                                            }
                                            .padding(horizontal = 24.dp, vertical = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    val coverageOptions = citiesByCountry[selectedCountryKey] ?: emptyList()

                    FilterChip(
                        selected = selectedCoverageKeys.isNotEmpty(),
                        onClick = { showCoverageSheet = true },
                        label = {
                            Text(
                                text = if (selectedCoverageKeys.isEmpty())
                                    stringResource(R.string.select_coverage)
                                else
                                    "${selectedCoverageKeys.size} ${stringResource(R.string.selected)}"
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.SettingsInputAntenna,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    )

                    if (showCoverageSheet) {
                        var draftCoverage by remember(selectedCoverageKeys) {
                            mutableStateOf(selectedCoverageKeys)
                        }

                        ModalBottomSheet(
                            onDismissRequest = { showCoverageSheet = false }
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(coverageOptions) { coverageItem ->
                                    val isChecked = coverageItem.key in draftCoverage

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                draftCoverage = if (isChecked) {
                                                    draftCoverage - coverageItem.key
                                                } else {
                                                    draftCoverage + coverageItem.key
                                                }
                                            }
                                            .padding(horizontal = 24.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(text = coverageItem.displayName)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    onCoverageSelected(draftCoverage)
                                    showCoverageSheet = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                            ) {
                                Text(stringResource(R.string.apply))
                            }
                        }
                    }
                }
            }
        }

        val isFilterActive =
            searchQuery.isNotEmpty() || selectedCountryKey != null || selectedCityKey != null
        val showPrompt = searchQuery.isEmpty() && selectedCountryKey == null
        if (showPrompt) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.search_or_filter_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            if (filteredStations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_stations_found))
                }
            } else {
                if (isFilterActive) {
                    LazyColumn(
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        items(
                            filteredStations,
                            key = { it.id ?: it.name ?: "" }) { station ->
                            RadioStationItem(
                                icon = station.icon ?: "",
                                name = station.name ?: "",
                                freq = station.freq ?: "",
                                city = station.stationCity ?: "",
                                location = station.location ?: "",
                                ps = station.ps ?: "",
                                rt = station.rt ?: "",
                                hasIssues = station.hasIssues ?: false,
                                isFavourite = favourites.any { it.id == station.id },
                                onFavouriteClick = { onFavouriteClick(station) },
                                onListenClick = { onListenClick(station) }
                            )
                        }
                    }
                }
            }
        }
    }
}