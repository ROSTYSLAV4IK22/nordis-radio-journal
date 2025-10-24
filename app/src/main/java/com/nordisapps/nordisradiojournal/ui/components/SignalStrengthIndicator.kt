package com.nordisapps.nordisradiojournal.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SignalStrengthIndicator(
    bitrate: Int?,
    modifier: Modifier = Modifier
) {
    val barHeights = listOf(12.dp, 18.dp, 24.dp, 30.dp)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        barHeights.forEachIndexed { index, height ->
            val isActive = isBarActive(index, bitrate)

            val color by animateColorAsState(
                targetValue = if (isActive) getColorForBitrate(bitrate) else Color.Gray.copy(alpha = 0.3f),
                animationSpec = tween(durationMillis = 500),
                label = "SignalBarColor"
            )

            Box(
                modifier = Modifier
                    .width(5.dp) // Ширина палочки
                    .height(height) // Высота из нашего массива
                    .clip(RoundedCornerShape(2.dp)) // Скругляем углы
                    .background(color)
            )
        }
    }
}

// Вспомогательная функция для определения, активна ли палочка
private fun isBarActive(index: Int, bitrate: Int?): Boolean {
    val currentBitrate = bitrate ?: 0
    return when (index) {
        0 -> currentBitrate >= 32
        1 -> currentBitrate >= 64
        2 -> currentBitrate >= 128
        3 -> currentBitrate >= 256
        else -> false
    }
}

// Вспомогательная функция для выбора цвета по битрейту
private fun getColorForBitrate(bitrate: Int?): Color {
    return when (bitrate ?: 0) {
        in 0..63 -> Color(0xFFD32F2F)   // Красный (плохое качество)
        in 64..127 -> Color(0xFFFBC02D)  // Желтый (среднее)
        in 128..255 -> Color(0xFF388E3C)  // Зеленый (хорошее)
        else -> Color(0xFF1976D2)        // Синий (отличное)
    }
}