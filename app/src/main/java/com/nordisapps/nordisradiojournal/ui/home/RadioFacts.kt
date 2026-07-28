package com.nordisapps.nordisradiojournal.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nordisapps.nordisradiojournal.data.model.RadioFact
import com.nordisapps.nordisradiojournal.data.model.getLocalizedText
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FactsCarousel(cards: List<RadioFact>, currentLanguage: String) {
    val pagerState = rememberPagerState(pageCount = { cards.size })

    LaunchedEffect(cards.size) {
        while (true) {
            if (cards.isEmpty()) {
                delay(500.milliseconds)
                continue
            }
            delay(5000.milliseconds)
            val nextPage = (pagerState.currentPage + 1) % cards.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState
        ) { page ->

            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),

                ) {
                Text(
                    text = cards[page].getLocalizedText(currentLanguage),
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        DotsIndicator(
            totalDots = cards.size,
            selectedIndex = pagerState.currentPage
        )
    }
}

@Composable
fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalDots) { index ->
            val color =
                if (index == selectedIndex)
                    Color.DarkGray
                else
                    Color.LightGray

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}