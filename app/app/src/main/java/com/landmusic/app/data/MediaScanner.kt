package com.landmusic.app.data

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.landmusic.app.qmc.QmcDecoder
import java.io.File

/** 扫描本地音频：MediaStore 标准音频 + 常见目录中的 QQ音乐加密文件 */
class MediaScanner(private val context: Context) {

    private val metaCache = MetadataCache(context)

    fun scanAll(): List<Song> {
        val standard = scanStandard()
        val encrypted = scanEncrypted()
        return (standard + encrypted)
            .distinctBy { it.path }
            .sortedByDescending { it.dateAddedSec }
    }

    /** MediaStore 中的常规音频（mp3/flac/m4a/ogg/wav 等） */
    private fun scanStandard(): List<Song> {
        val out = ArrayList<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val sort = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        runCatching {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                sort
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val path = c.getString(dataCol) ?: continue
                    val uri = android.content.ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    ).toString()
                    out += Song(
                        id = "ms_$id",
                        title = c.getString(titleCol) ?: File(path).nameWithoutExtension,
                        artist = c.getString(artistCol)?.takeUnless { it == "<unknown>" } ?: "未知艺人",
                        album = c.getString(albumCol) ?: "未知专辑",
                        durationMs = c.getLong(durCol),
                        path = path,
                        mediaUri = uri,
                        isEncrypted = false,
                        dateAddedSec = c.getLong(dateCol),
                    )
                }
            }
        }
        return out
    }

    /** 常见目录中的 QQ音乐加密文件（需要「所有文件访问」权限才能看到） */
    private fun scanEncrypted(): List<Song> {
        val root = Environment.getExternalStorageDirectory()
        val candidates = listOf(
            File(root, "qqmusic/song"),
            File(root, "qqmusic"),
            File(root, "QQMusic"),
            File(root, "Music"),
            File(root, "Download"),
        )
        val found = LinkedHashMap<String, File>()
        for (dir in candidates) {
            if (!dir.exists() || !dir.canRead()) continue
            runCatching {
                dir.walkTopDown()
                    .maxDepth(4)
                    .filter { it.isFile && QmcDecoder.isEncryptedFile(it.name) }
                    .forEach { found.putIfAbsent(it.absolutePath, it) }
            }
        }
        return found.values.map { f ->
            val (artist, title) = parseQqFilename(f.nameWithoutExtension)
            val meta = metaCache.get(f.absolutePath)
            Song(
                id = "qmc_${MetadataCache.pathHash(f.absolutePath)}",
                title = meta?.title ?: title,
                artist = meta?.artist ?: artist,
                album = meta?.album ?: "未知专辑",
                durationMs = meta?.durationMs ?: 0L,
                path = f.absolutePath,
                mediaUri = null,
                isEncrypted = true,
                dateAddedSec = f.lastModified() / 1000,
            )
        }
    }

    /** "张天赋 - 小心地滑 [mqms2]" -> ("张天赋", "小心地滑") */
    private fun parseQqFilename(name: String): Pair<String, String> {
        var n = name.replace(Regex("\\s*\\[[^]]*]\\s*$"), "").trim()
        val idx = n.indexOf(" - ")
        return if (idx > 0) {
            n.substring(0, idx).trim() to n.substring(idx + 3).trim()
        } else {
            "未知艺人" to n.ifBlank { name }
        }
    }
}
