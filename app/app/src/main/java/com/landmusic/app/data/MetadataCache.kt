package com.landmusic.app.data

import android.content.Context
import android.media.MediaMetadataRetriever
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/** 加密文件首次解密后提取的元数据缓存（标题/艺人/专辑/时长），JSON 持久化 */
class MetadataCache(context: Context) {

    private val file = File(context.filesDir, "qmc_meta.json")
    private val lock = Any()
    private var map: MutableMap<String, JSONObject> = load()

    data class Meta(
        val title: String?,
        val artist: String?,
        val album: String?,
        val durationMs: Long,
        val ext: String,
    )

    @Synchronized
    fun get(path: String): Meta? = map[path]?.let {
        Meta(
            title = it.optString("title").ifBlank { null },
            artist = it.optString("artist").ifBlank { null },
            album = it.optString("album").ifBlank { null },
            durationMs = it.optLong("duration", 0L),
            ext = it.optString("ext", "flac"),
        )
    }

    fun put(path: String, meta: Meta) = synchronized(lock) {
        map[path] = JSONObject().apply {
            put("title", meta.title ?: "")
            put("artist", meta.artist ?: "")
            put("album", meta.album ?: "")
            put("duration", meta.durationMs)
            put("ext", meta.ext)
        }
        save()
    }

    /** 从解密后的文件读取标签 */
    fun extractAndPut(path: String, decrypted: File, ext: String, fallbackTitle: String, fallbackArtist: String): Meta {
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var duration = 0L
        runCatching {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(decrypted.absolutePath)
            title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            mmr.release()
        }
        val meta = Meta(
            title = title ?: fallbackTitle,
            artist = artist ?: fallbackArtist,
            album = album,
            durationMs = duration,
            ext = ext,
        )
        put(path, meta)
        return meta
    }

    private fun load(): MutableMap<String, JSONObject> {
        if (!file.exists()) return mutableMapOf()
        return runCatching {
            val root = JSONObject(file.readText())
            val out = mutableMapOf<String, JSONObject>()
            root.keys().forEach { k -> out[k] = root.getJSONObject(k) }
            out
        }.getOrDefault(mutableMapOf())
    }

    private fun save() {
        runCatching {
            val root = JSONObject()
            map.forEach { (k, v) -> root.put(k, v) }
            file.writeText(root.toString())
        }
    }

    companion object {
        fun pathHash(path: String): String {
            val md = MessageDigest.getInstance("MD5")
            return md.digest(path.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
