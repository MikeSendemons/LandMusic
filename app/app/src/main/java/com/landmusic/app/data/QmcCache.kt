package com.landmusic.app.data

import android.content.Context
import com.landmusic.app.qmc.QmcDecoder
import java.io.File

/**
 * 加密文件解密缓存管理：解密结果写入 filesDir/decrypted/<pathHash>.<ext>，
 * 已缓存的直接复用；同时负责首次解密后的元数据提取。
 */
class QmcCache(context: Context) {

    private val dir = File(context.filesDir, "decrypted").apply { mkdirs() }
    private val metaCache = MetadataCache(context)

    fun cachedFileFor(path: String): File? {
        val meta = metaCache.get(path) ?: return null
        val f = File(dir, "${MetadataCache.pathHash(path)}.${meta.ext}")
        return if (f.exists() && f.length() > 0) f else null
    }

    /** 同步解密（在 IO 线程调用）。失败抛出异常。 */
    @Synchronized
    fun decryptToCache(path: String, fallbackTitle: String, fallbackArtist: String): File {
        cachedFileFor(path)?.let { return it }
        val src = File(path)
        require(src.exists()) { "源文件不存在: $path" }
        val result = QmcDecoder.decrypt(src)
        val out = File(dir, "${MetadataCache.pathHash(path)}.${result.ext}")
        out.writeBytes(result.audio)
        metaCache.extractAndPut(path, out, result.ext, fallbackTitle, fallbackArtist)
        return out
    }

    fun meta(path: String): MetadataCache.Meta? = metaCache.get(path)

    /** 清理源文件已不存在的缓存 */
    fun prune(validPaths: Set<String>) {
        dir.listFiles()?.forEach { f ->
            // 无法从 hash 反查路径时保守保留；仅清理超出大小限制的最旧文件
        }
        val files = dir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        val limit = 1024L * 1024 * 1024 // 1GB
        for (f in files) {
            if (total <= limit) break
            total -= f.length()
            f.delete()
        }
    }
}
