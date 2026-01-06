package com.nordisapps.nordisradiojournal

data class Announcement(
    val enabled: Boolean = false,
    val priority: Int = 0,
    val startDate: String = "",
    val endDate: String = "",
    val emoji: String = "",
    val imageUrl: String? = "",
    val title: String = "",
    val description: String = "",
    val actionText: String = "",
    val actionType: String = "",
    val actionValue: String = ""
)