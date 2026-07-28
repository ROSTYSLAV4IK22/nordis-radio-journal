package com.nordisapps.nordisradiojournal

import android.app.Application
import coil.ImageLoader
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import com.nordisapps.nordisradiojournal.viewmodel.SharedStateHolder

class MyApp : Application() {
    lateinit var imageLoader: ImageLoader
        private set

    val sharedState = SharedStateHolder()

    override fun onCreate() {
        super.onCreate()

        imageLoader = ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}