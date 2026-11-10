package com.landmusic.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.landmusic.app.artwork.ArtworkFetcher
import com.landmusic.app.data.QmcCache
import com.landmusic.app.playback.PlayerManager

class LandMusicApp : Application(), ImageLoaderFactory {

    lateinit var qmcCache: QmcCache
        private set
    lateinit var playerManager: PlayerManager
        private set

    override fun onCreate() {
        super.onCreate()
        qmcCache = QmcCache(this)
        playerManager = PlayerManager(this)
        playerManager.ensureConnected()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(ArtworkFetcher.Factory(this@LandMusicApp, qmcCache)) }
        .crossfade(true)
        .build()
}
