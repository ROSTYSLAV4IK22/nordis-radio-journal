package com.nordisapps.nordisradiojournal.data.model

data class Announcement(
    val id: String = "",
    val enabled: Boolean = false,
    val type: String = "news",
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
) {
    val typeEnum: AnnouncementType
        get() = AnnouncementType.fromString(type)
}