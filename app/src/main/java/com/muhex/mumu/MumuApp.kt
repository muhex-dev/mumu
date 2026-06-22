package com.muhex.mumu

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class   MumuApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AppIconFetcher.Factory(this@MumuApp))
            }
            .crossfade(true)
            .build()
    }
}
