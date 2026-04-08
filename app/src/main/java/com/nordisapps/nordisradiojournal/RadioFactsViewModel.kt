package com.nordisapps.nordisradiojournal

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.collections.filter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RadioFactsViewModel : ViewModel() {
    private val _facts = MutableStateFlow<List<RadioFact>>(emptyList())
    val facts = _facts.asStateFlow()

    fun loadFacts() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("radio_facts")
            .get()
            .addOnSuccessListener { documents ->
                val factsList = documents.map { doc ->
                    doc.toObject(RadioFact::class.java)
                }.filter { fact ->
                    val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val factDate = format.parse(fact.date) ?: return@filter false
                    val today = Date()

                    val diffMs = today.time - factDate.time
                    val diffDays = diffMs / (1000 * 60 * 60 * 24)

                    diffDays <= 7
                }
                _facts.value = factsList
            }
    }
}