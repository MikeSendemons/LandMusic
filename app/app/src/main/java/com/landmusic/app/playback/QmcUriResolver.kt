package com.landmusic.app.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.landmusic.app.data.QmcCache
import java.io.File

/**
 * 将 landmusic://qmc/?path=<加密文件路径> 解析为解密缓存文件的 file:// URI。
 * ExoPlayer 在加载线程同步调用，解密为一次性成本，之后走缓存。
 */
@UnstableApi
class QmcUriResolver(private val qmcCache: QmcCache) : ResolvingDataSource.Resolver {

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val uri = dataSpec.uri
        if (uri.scheme != SCHEME) return dataSpec
        val path = uri.getQueryParameter("path")
            ?: throw IllegalArgumentException("landmusic uri 缺少 path 参数")
        val fallbackTitle = File(path).nameWithoutExtension.substringAfter(" - ")
        val cached = qmcCache.decryptToCache(path, fallbackTitle, "未知艺人")
        return dataSpec.withUri(Uri.fromFile(cached))
    }

    companion object {
        const val SCHEME = "landmusic"

        fun uriFor(path: String): Uri = Uri.Builder()
            .scheme(SCHEME)
            .authority("qmc")
            .appendQueryParameter("path", path)
            .build()
    }
}
