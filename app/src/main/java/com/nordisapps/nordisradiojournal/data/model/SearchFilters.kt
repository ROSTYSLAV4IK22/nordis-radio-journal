package com.nordisapps.nordisradiojournal.data.model

data class SearchFilters(
    val query: String = "",
    val country: String? = null,
    val city: String? = null,
    val coverage: Set<String> = emptySet(),
    val category: Set<String> = emptySet()
)