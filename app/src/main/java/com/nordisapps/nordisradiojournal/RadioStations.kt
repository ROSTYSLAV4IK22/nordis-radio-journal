package com.nordisapps.nordisradiojournal

import com.google.firebase.database.FirebaseDatabase

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

fun loadStations(onLoaded: (List<Station>) -> Unit) {
    stationsRef.get().addOnSuccessListener { snapshot ->
        val stations = snapshot.children.mapNotNull { child ->
            val station = child.getValue(Station::class.java)
            station?.copy(id = child.key)
        }
        onLoaded(stations)
    }
}