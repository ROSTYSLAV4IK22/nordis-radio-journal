package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AppViewModelFactory(
    private val application: Application,
    private val shared: SharedStateHolder
) : ViewModelProvider.AndroidViewModelFactory(application) {

    private var stationsViewModel: StationsViewModel? = null
    private var favouritesViewModel: FavouritesViewModel? = null

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            PlayerViewModel::class.java -> PlayerViewModel(application, shared) as T
            StationsViewModel::class.java -> {
                val vm = StationsViewModel(application, shared)
                stationsViewModel = vm
                vm as T
            }
            FavouritesViewModel::class.java -> FavouritesViewModel(application, shared) as T
            RecentlyPlayedViewModel::class.java -> RecentlyPlayedViewModel(application, shared) as T
            AdminViewModel::class.java -> AdminViewModel(
                application,
                shared,
                onStationsChanged = { stationsViewModel?.loadStations() }
            ) as T
            AuthViewModel::class.java -> AuthViewModel(
                application,
                shared,
                onUserLoggedIn = { uid -> favouritesViewModel?.mergeFavouritesOnLogin(uid) }
            ) as T
            AnnouncementsViewModel::class.java -> AnnouncementsViewModel(application, shared) as T
            else -> super.create(modelClass)
        }
    }
}