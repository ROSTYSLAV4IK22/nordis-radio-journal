package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.database.FirebaseDatabase
import com.nordisapps.nordisradiojournal.data.Station

class AdminViewModel(
    application: Application,
    private val shared: SharedStateHolder,
    private val onStationsChanged: () -> Unit
) : AndroidViewModel(application) {

    fun deleteStation(station: Station, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val stationId = station.id
        if (stationId.isNullOrEmpty()) {
            onFailure(Exception("Cannot delete station with empty ID"))
            return
        }
        val dbRef = FirebaseDatabase.getInstance().getReference("stations")
        dbRef.child(stationId).removeValue()
            .addOnSuccessListener {
                Log.d("AdminViewModel", "Station deleted from Firebase: $stationId")
                val updated = shared.uiState.value.stations.filterNot { it.id == stationId }
                shared.update { it.copy(stations = updated) }
                onSuccess()
            }
            .addOnFailureListener { error ->
                Log.e("AdminViewModel", "Failed to delete station: ${error.message}")
                onFailure(error)
            }
    }

    fun saveStation(station: Station, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val dbref = FirebaseDatabase.getInstance().getReference("stations")
        val stationId = station.id

        if (stationId.isNullOrEmpty()) {
            dbref.get().addOnSuccessListener { snapshot ->
                val stations = snapshot.children.mapNotNull { it.getValue(Station::class.java) }
                val maxDisplayId = stations.maxOfOrNull { it.displayId ?: 0 } ?: 0
                val newDisplayId = maxDisplayId + 1

                val newStationRef = dbref.push()
                val newStation = station.copy(
                    id = newStationRef.key,
                    displayId = newDisplayId
                )

                newStationRef.setValue(newStation)
                    .addOnSuccessListener {
                        Log.d("AdminViewModel", "Station created with displayId $newDisplayId")
                        onStationsChanged()
                        onSuccess()
                    }
            }.addOnFailureListener { error -> onFailure(error) }
        } else {
            dbref.child(stationId).setValue(station)
                .addOnSuccessListener {
                    Log.d("AdminViewModel", "Station data saved successfully")
                    onStationsChanged()
                    onSuccess()
                }
        }.addOnFailureListener { error -> onFailure(error) }
    }
}