package com.nordisapps.nordisradiojournal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.nordisapps.nordisradiojournal.ui.home.FavoritesTab
import com.nordisapps.nordisradiojournal.ui.home.HomeTab
import com.nordisapps.nordisradiojournal.ui.home.ListenTab
import com.nordisapps.nordisradiojournal.ui.home.SearchTab

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    factsViewModel: RadioFactsViewModel,
    currentLanguage: String,
    selectedTab: Int
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCountryKey by viewModel.selectedCountry.collectAsState()
    val selectedCityKey by viewModel.selectedCity.collectAsState()
    val filteredStations by viewModel.filteredStations.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val favourites = uiState.favouriteStations
    val facts = factsViewModel.facts.collectAsState().value

    Column(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> {
                HomeTab(
                    isLoading = uiState.isLoading,
                    facts = facts,
                    christmasDeco = viewModel.christmasDeco,
                    onFactsLoad = { factsViewModel.loadFacts() },
                    currentLanguage = currentLanguage
                )
            }

            1 -> {
                SearchTab(
                    searchQuery = searchQuery,
                    selectedCountryKey = selectedCountryKey,
                    selectedCityKey = selectedCityKey,
                    filteredStations = filteredStations,
                    favourites = favourites,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onCountrySelected = { viewModel.setSelectedCountry(it) },
                    onCitySelected = {viewModel.setSelectedCity(it) },
                    onFavouriteClick = { viewModel.toggleFavourite(it) },
                    onListenClick = { viewModel.playStation(it) }
                )
            }

            2 -> {
                FavoritesTab(
                    favourites = favourites,
                    onFavouriteClick = { viewModel.toggleFavourite(it) },
                    onStationClick = { viewModel.playStation(it) }
                )
            }

            3 -> {
                ListenTab(
                    recentlyPlayed = uiState.recentlyPlayedStations,
                    onStationClick = { viewModel.playStation(it) }
                )
            }
        }
    }
}