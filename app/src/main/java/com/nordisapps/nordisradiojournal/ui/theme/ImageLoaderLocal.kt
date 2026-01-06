package com.nordisapps.nordisradiojournal.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import coil.ImageLoader

val LocalImageLoader = staticCompositionLocalOf<ImageLoader> {
    error("Image Loader not provided")
}