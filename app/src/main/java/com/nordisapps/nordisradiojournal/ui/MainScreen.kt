package com.nordisapps.nordisradiojournal.ui

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
import com.nordisapps.nordisradiojournal.viewmodel.AnnouncementsViewModel
import com.nordisapps.nordisradiojournal.viewmodel.FavouritesViewModel
import com.nordisapps.nordisradiojournal.viewmodel.PlayerViewModel
import com.nordisapps.nordisradiojournal.viewmodel.RadioFactsViewModel
import com.nordisapps.nordisradiojournal.viewmodel.RecentlyPlayedViewModel
import com.nordisapps.nordisradiojournal.viewmodel.StationsViewModel

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    playerViewModel: PlayerViewModel,
    stationsViewModel: StationsViewModel,
    recentlyPlayedViewModel: RecentlyPlayedViewModel,
    favouritesViewModel: FavouritesViewModel,
    announcementsViewModel: AnnouncementsViewModel,
    factsViewModel: RadioFactsViewModel,
    currentLanguage: String,
    selectedTab: Int
) {
    val filters by stationsViewModel.filters.collectAsState()
    val filteredStations by stationsViewModel.filteredStations.collectAsState()
    val uiState by stationsViewModel.uiStateFlow.collectAsState()
    val favourites = uiState.favouriteStations
    val facts = factsViewModel.facts.collectAsState().value

    Column(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> {
                HomeTab(
                    isLoading = uiState.isLoading,
                    facts = facts,
                    christmasDeco = announcementsViewModel.christmasDeco,
                    onFactsLoad = { factsViewModel.loadFacts() },
                    currentLanguage = currentLanguage
                )
            }

            1 -> {
                SearchTab(
                    searchQuery = filters.query,
                    selectedCountryKey = filters.country,
                    selectedCityKey = filters.city,
                    selectedCoverageKeys = filters.coverage,
                    selectedCategoryKeys = filters.category,
                    filteredStations = filteredStations,
                    favourites = favourites,
                    onSearchQueryChange = { stationsViewModel.setSearchQuery(it) },
                    onCountrySelected = { stationsViewModel.setSelectedCountry(it) },
                    onCitySelected = { stationsViewModel.setSelectedCity(it) },
                    onCoverageSelected = { stationsViewModel.setSelectedCoverage(it) },
                    onFavouriteClick = { favouritesViewModel.toggleFavourite(it) },
                    onListenClick = { station ->
                        playerViewModel.playStation(station) { recentlyPlayedViewModel.addStationToHistory(it) }
                    },
                    onCategorySelected = { stationsViewModel.setSelectedCategory(it) }
                )
            }

            2 -> {
                FavoritesTab(
                    favourites = favourites,
                    onFavouriteClick = { favouritesViewModel.toggleFavourite(it) },
                    onStationClick = { station ->
                        playerViewModel.playStation(station) { recentlyPlayedViewModel.addStationToHistory(it) }
                    }
                )
            }

            3 -> {
                ListenTab(
                    recentlyPlayed = uiState.recentlyPlayedStations,
                    onStationClick = { station ->
                        playerViewModel.playStation(station) { recentlyPlayedViewModel.addStationToHistory(it) }
                    }
                )
            }
        }
    }
}