package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nordisapps.nordisradiojournal.data.FAVORITE_STATIONS_KEY
import com.nordisapps.nordisradiojournal.data.Station
import com.nordisapps.nordisradiojournal.data.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.collections.contains

class FavouritesViewModel(
    application: Application,
    private val shared: SharedStateHolder
) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext

    fun toggleFavourite(station: Station) {
        val currentFavourites = shared.uiState.value.favouriteStations.toMutableList()
        if (currentFavourites.any { it.id == station.id }) {
            currentFavourites.removeAll { it.id == station.id }
        } else {
            currentFavourites.add(station)
        }
        shared.update { it.copy(favouriteStations = currentFavourites) }
        saveFavourites()
    }

    private fun saveFavourites() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val favoriteIds = shared.uiState.value.favouriteStations.mapNotNull { it.id }.toSet()
            if (user != null) {
                FirebaseDatabase.getInstance()
                    .getReference("favorites")
                    .child(user.uid)
                    .setValue(favoriteIds.toList())
            } else {
                context.dataStore.edit { preferences ->
                    preferences[FAVORITE_STATIONS_KEY] = favoriteIds
                }
            }
        }
    }

    fun loadFavourites() {
        viewModelScope.launch {
            val user = FirebaseAuth.getInstance().currentUser
            val stations = shared.uiState
                .map { it.stations }
                .first { it.isNotEmpty() }

            if (user != null) {
                FirebaseDatabase.getInstance()
                    .getReference("favorites")
                    .child(user.uid)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val favoriteIds =
                            snapshot.children.mapNotNull { it.getValue(String::class.java) }
                        val favStations = stations.filter { it.id in favoriteIds }
                        shared.update { it.copy(favouriteStations = favStations) }
                    }
            } else {
                val preferences = context.dataStore.data.first()
                val favoriteIds = preferences[FAVORITE_STATIONS_KEY] ?: emptySet()
                val favStations = stations.filter { it.id in favoriteIds }
                shared.update { it.copy(favouriteStations = favStations) }
            }
        }
    }

    fun mergeFavouritesOnLogin(uid: String) {
        viewModelScope.launch {
            val preferences = context.dataStore.data.first()
            val localIds = preferences[FAVORITE_STATIONS_KEY] ?: emptySet()

            val stations = shared.uiState
                .map { it.stations }
                .first { it.isNotEmpty() }

            if (localIds.isEmpty()) {
                loadFavourites()
                return@launch
            }

            val ref = FirebaseDatabase.getInstance()
                .getReference("favorites")
                .child(uid)

            ref.get().addOnSuccessListener { snapshot ->
                val firebaseIds = snapshot.children
                    .mapNotNull { it.getValue(String::class.java) }
                    .toSet()

                val mergedIds = (localIds + firebaseIds).toList()

                ref.setValue(mergedIds).addOnSuccessListener {
                    viewModelScope.launch {
                        context.dataStore.edit { prefs ->
                            prefs[FAVORITE_STATIONS_KEY] = emptySet()
                        }
                    }
                    val favStations = stations.filter { it.id in mergedIds }
                    shared.update { it.copy(favouriteStations = favStations) }
                }
            }
        }
    }
}