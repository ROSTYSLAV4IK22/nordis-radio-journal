package com.nordisapps.nordisradiojournal.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Snowflake(
    val x: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun SnowOverlay(
    enabled: Boolean,
    snowCount: Int = 80
) {
    if (!enabled) return

    val flakes = remember {
        List(snowCount) {
            Snowflake(
                x = Random.nextFloat(),
                startY = Random.nextFloat(),
                size = Random.nextFloat() * 2.5f + 1.5f,
                speed = Random.nextFloat() * 0.25f + 0.1f,
                alpha = Random.nextFloat() * 0.15f + 0.15f
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "snow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14_000, easing = LinearEasing)
    ),
        label = "snow_progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        flakes.forEach { flake ->
            val y = (flake.startY * height + progress * height * flake.speed) % height
            val x = (flake.x * width + progress * 20f * flake.speed) % width

            drawCircle(
                color = Color(0xFFE6ECF2).copy(alpha = flake.alpha),
                radius = flake.size,
                center = Offset(x, y)
            )
        }
    }
}