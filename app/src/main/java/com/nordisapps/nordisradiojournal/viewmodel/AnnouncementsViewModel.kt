package com.nordisapps.nordisradiojournal.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.nordisapps.nordisradiojournal.data.model.Announcement
import com.nordisapps.nordisradiojournal.data.model.AnnouncementType
import java.time.LocalDate
import java.time.Month

class AnnouncementsViewModel(
    application: Application,
    private val shared: SharedStateHolder
) : AndroidViewModel(application) {

    private companion object {
        const val ENABLE_ANNOUNCEMENTS = true
    }

    private val firestore = FirebaseFirestore.getInstance()

    var announcements by mutableStateOf<List<Announcement>>(emptyList())
        private set

    var christmasDeco by mutableStateOf<Announcement?>(null)
        private set

    val isChristmas = mutableStateOf(checkChristmas())

    init {
        if (ENABLE_ANNOUNCEMENTS) {
            loadAnnouncement()
        }
        loadChristmasDeco()
    }

    private fun checkChristmas(): Boolean {
        val today = LocalDate.now()

        val start = if (today.month >= Month.DECEMBER) {
            LocalDate.of(today.year, Month.DECEMBER, 1)
        } else {
            LocalDate.of(today.year - 1, Month.DECEMBER, 1)
        }
        val end = start.plusMonths(2)

        return !today.isBefore(start) && today.isBefore(end)
    }

    private fun loadAnnouncement() {
        firestore.collection("announcements")
            .whereEqualTo("enabled", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val today = LocalDate.now()

                val active = snapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(Announcement::class.java)?.copy(id = doc.id)
                    }
                    .filter { it.typeEnum != AnnouncementType.CHRISTMAS }
                    .filter { isInDateRange(it, today) }
                    .sortedWith(compareByDescending<Announcement> { it.priority }
                        .thenByDescending { it.startDate })

                announcements = active
                shared.update {
                    it.copy(isLoading = false)
                }
            }
            .addOnFailureListener {
                shared.update {
                    it.copy(isLoading = false)
                }
            }
    }

    private fun isInDateRange(a: Announcement, today: LocalDate): Boolean {
        val start = LocalDate.parse(a.startDate)
        val end = LocalDate.parse(a.endDate)
        return !today.isBefore(start) && today.isBefore(end)
    }

    private fun loadChristmasDeco() {
        firestore.collection("announcements")
            .whereEqualTo("enabled", true)
            .whereEqualTo("type", "christmas")
            .get()
            .addOnSuccessListener { snapshot ->
                val today = LocalDate.now()

                christmasDeco = snapshot.documents
                    .mapNotNull { it.toObject(Announcement::class.java) }
                    .firstOrNull {
                        !today.isBefore(LocalDate.parse(it.startDate)) && today.isBefore(
                            LocalDate.parse(it.endDate)
                        )
                    }
            }
    }
}