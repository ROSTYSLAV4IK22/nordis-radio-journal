package com.nordisapps.nordisradiojournal.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nordisapps.nordisradiojournal.Announcement
import com.nordisapps.nordisradiojournal.R
import com.nordisapps.nordisradiojournal.RadioFact

@Composable
fun HomeTab(
    isLoading: Boolean,
    christmasDeco: Announcement?,
    facts: List<RadioFact>,
    currentLanguage: String,
    onFactsLoad: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.current_events),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                }

                christmasDeco != null -> {
                    val deco = christmasDeco
                    ChristmasDecoCard(
                        imageUrl = deco.imageUrl.orEmpty(),
                        title = deco.title,
                        description = deco.description
                    )
                }

                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_active_events),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.facts),
            style = MaterialTheme.typography.titleMedium
        )

        LaunchedEffect(Unit) {
            onFactsLoad()
        }

        FactsCarousel(
            cards = facts,
            currentLanguage = currentLanguage
        )
    }
}