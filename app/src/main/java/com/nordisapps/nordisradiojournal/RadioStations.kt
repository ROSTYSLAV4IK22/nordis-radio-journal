package com.nordisapps.nordisradiojournal

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

data class Station(
    val id: String? = null,
    val displayId: Int? = null,
    val name: String? = null,
    val freq: String? = null,
    val ps: String? = null,
    val rt: String? = null,
    val icon: String? = null,
    val stream: String? = null,
    val country: String? = null,
    val mainCity: String? = null,
    val stationCity: String? = null,
    val location: String? = null
)

val database = FirebaseDatabase.getInstance()
val stationsRef = database.getReference("stations")

suspend fun loadStations(): List<Station> {
    return try {
        // Используем .await() для ожидания результата без блокировки потока
        val snapshot = stationsRef.get().await()

        val stations = snapshot.children.mapNotNull { child ->
            val station = child.getValue(Station::class.java)
            station?.copy(id = child.key)
        }
        stations.sortedBy { it.displayId }
    } catch (e: Exception) {
        Log.e("RadioStations", "Error loading stations from Firebase", e)
        emptyList()
    }
}