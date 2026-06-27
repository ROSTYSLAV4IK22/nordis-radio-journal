package com.nordisapps.nordisradiojournal.ui.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nordisapps.nordisradiojournal.R

@Composable
fun rememberCategoryDisplayNames(): Map<String, String> = mapOf(
    "Music" to stringResource(R.string.category_music),
    "News" to stringResource(R.string.category_news),
    "Talk" to stringResource(R.string.category_talk),
    "Church" to stringResource(R.string.category_church),
    "Children" to stringResource(R.string.category_children),
    "Sports" to stringResource(R.string.category_sports),
    "Cultural" to stringResource(R.string.category_cultural),
    "Regional" to stringResource(R.string.category_regional)
)