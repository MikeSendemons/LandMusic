package com.landmusic.app.artwork

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import com.landmusic.app.data.MetadataCache
import com.landmusic.app.data.QmcCache
import com.landmusic.app.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** 封面提取：内嵌图片优先，结果缓存到 filesDir/artwork；加密歌曲在首次解密后可用 */
object ArtworkLoader {

    fun load(context: Context, song: Song, qmcCache: QmcCache): ByteArray? {
        val dir = File(context.filesDir, "artwork").apply { mkdirs() }
        val cacheFile = File(dir, MetadataCache.pathHash(song.path) + ".img")
        if (cacheFile.exists() && cacheFile.length() > 0) {
            return runCatching { cacheFile.readBytes() }.getOrNull()
        }
        val bytes = runCatching {
            val mmr = MediaMetadataRetriever()
            if (song.isEncrypted) {
                val decrypted = qmcCache.cachedFileFor(song.path) ?: return null
                mmr.setDataSource(decrypted.absolutePath)
            } else if (song.mediaUri != null) {
                mmr.setDataSource(context, Uri.parse(song.mediaUri))
            } else {
                mmr.setDataSource(song.path)
            }
            val pic = mmr.embeddedPicture
            mmr.release()
            pic
        }.getOrNull()
        if (bytes != null) {
            runCatching { cacheFile.writeBytes(bytes) }
        }
        return bytes
    }
}

class ArtworkFetcher(
    private val song: Song,
    private val context: Context,
    private val qmcCache: QmcCache,
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bytes = withContext(Dispatchers.IO) {
            ArtworkLoader.load(context, song, qmcCache)
        } ?: return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return DrawableResult(
            drawable = bitmap.toDrawable(context.resources),
            isSampled = false,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(
        private val context: Context,
        private val qmcCache: QmcCache,
    ) : Fetcher.Factory<Song> {
        override fun create(data: Song, options: Options, imageLoader: ImageLoader): Fetcher =
            ArtworkFetcher(data, context, qmcCache)
    }
}
