package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nordisapps.nordisradiojournal.data.model.SearchFilters
import com.nordisapps.nordisradiojournal.data.model.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.any
import com.nordisapps.nordisradiojournal.data.loadStations as fetchStationsFromNetwork

class StationsViewModel(
    application: Application,
    private val shared: SharedStateHolder
) : AndroidViewModel(application) {

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters
    val uiStateFlow: StateFlow<UiState> = shared.uiState

    init {
        loadStations()
    }

    fun setSearchQuery(query: String) {
        _filters.update { it.copy(query = query) }
    }

    fun setSelectedCountry(country: String?) {
        _filters.update { it.copy(country = country, city = null, coverage = emptySet()) }
    }

    fun setSelectedCity(city: String?) {
        _filters.update {
            it.copy(
                city = city,
                coverage = if (city != null) setOf(city) else emptySet()
            )
        }
    }

    fun setSelectedCoverage(coverage: Set<String>) {
        _filters.update {
            it.copy(
                coverage = coverage,
                city = if (coverage.size == 1) coverage.first() else null
            )
        }
    }

    fun setSelectedCategory(category: Set<String>) {
        _filters.update {
            it.copy(
                category = category
            )
        }
    }

    val filteredStations = combine(
        shared.uiState,
        _filters
    ) { state, filters ->
        val tokens = filters.query
            .trim()
            .lowercase()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        state.stations.filter { station ->
            val name = station.name?.lowercase().orEmpty()
            val cityName = station.stationCity?.lowercase().orEmpty()
            val mainCity = station.mainCity?.lowercase().orEmpty()
            val countryName = station.country?.lowercase().orEmpty()

            val matchesQuery = tokens.isEmpty() || tokens.all { token ->
                name.contains(token) ||
                        cityName.contains(token) ||
                        mainCity.contains(token) ||
                        countryName.contains(token)
            }

            val matchesCountry = filters.country.isNullOrBlank() || station.country?.equals(
                filters.country,
                ignoreCase = true
            ) == true

            val matchesCity = if (filters.coverage.isNotEmpty()) {
                true
            } else {
                filters.city.isNullOrBlank() || station.mainCity?.equals(
                    filters.city,
                    ignoreCase = true
                ) == true
            }

            val matchesCoverage =
                filters.coverage.isEmpty() || station.coverage?.any { it in filters.coverage } == true
            val matchesCategory =
                filters.category.isEmpty() || station.category?.any { it in filters.category } == true

            matchesQuery && matchesCountry && matchesCity && matchesCoverage && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadStations() {
        viewModelScope.launch {
            try {
                shared.update { it.copy(isLoading = true) }

                val stationList = fetchStationsFromNetwork()

                shared.update { it.copy(stations = stationList) }
            } catch (e: Exception) {
                Log.e("StationsViewModel", "Error loading stations", e)
            } finally {
                shared.update { it.copy(isLoading = false) }
            }
        }
    }
}