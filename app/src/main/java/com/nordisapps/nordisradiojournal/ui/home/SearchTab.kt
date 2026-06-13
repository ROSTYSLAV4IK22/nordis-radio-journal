@file:Suppress("AssignedValueIsNeverRead")

package com.nordisapps.nordisradiojournal.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import kotlin.collections.find
import kotlin.collections.forEach

data class LocationItem(val key: String, val displayName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTab(
    searchQuery: String,
    selectedCountryKey: String?,
    selectedCityKey: String?,
    filteredStations: List<Station>,
    favourites: List<Station>,
    onSearchQueryChange: (String) -> Unit,
    onCountrySelected: (String) -> Unit,
    onCitySelected: (String?) -> Unit,
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

    var expandedCountry by remember { mutableStateOf(false) }

    var expandedCity by remember { mutableStateOf(false) }

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
                    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCountry,
                        onDismissRequest = { expandedCountry = false }
                    ) {
                        countries.forEach { countryItem ->
                            DropdownMenuItem(
                                text = { Text(countryItem.displayName) },
                                onClick = {
                                    onCountrySelected(countryItem.key)
                                    onCitySelected(null)
                                    expandedCountry = false
                                }
                            )
                        }
                    }
                }

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
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCity,
                            onDismissRequest = { expandedCity = false }
                        ) {
                            cities.forEach { cityItem ->
                                DropdownMenuItem(
                                    text = { Text(cityItem.displayName) },
                                    onClick = {
                                        onCitySelected(cityItem.key)
                                        expandedCity = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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