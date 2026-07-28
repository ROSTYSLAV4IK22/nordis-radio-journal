package com.nordisapps.nordisradiojournal.data.model

@Suppress("PropertyName")
data class RadioFact(
    val text_en: String = "",
    val text_ro: String = "",
    val text_ru: String = "",
    val text_uk: String = "",
    val category: String = "",
    val image: String = "",
    val date: String = ""
)

fun RadioFact.getLocalizedText(language: String): String {
    return when (language) {
        "ru" -> text_ru
        "ro" -> text_ro
        "uk" -> text_uk
        else -> text_en
    }
}