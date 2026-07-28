package com.nordisapps.nordisradiojournal.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.ui.graphics.vector.ImageVector

enum class AnnouncementType(val icon: ImageVector) {
    FEATURE(Icons.Default.AutoAwesome),
    FIX(Icons.Default.BugReport),
    NEWS(Icons.Default.Campaign),
    EVENT(Icons.Default.Event),
    CHRISTMAS(Icons.Default.AcUnit);

    companion object {
        fun fromString(value: String): AnnouncementType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: NEWS
    }
}