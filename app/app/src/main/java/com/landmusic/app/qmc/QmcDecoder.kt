package com.landmusic.app.qmc

import java.io.File

/**
 * QMC 文件解码器：解析文件尾部的密钥布局（QTag / V1 内嵌 / 静态表），
 * 解密音频主体并嗅探真实格式。
 * 移植自 unlock-music src/decrypt/qmc.ts（QmcDecoder）。
 */
object QmcDecoder {

    val ENCRYPTED_EXTENSIONS = setOf(
        "mgg", "mgg0", "mgg1", "mgg2", "mggl",
        "mflac", "mflac0", "mflac2", "mflach",
        "qmc0", "qmc2", "qmc3", "qmcflac", "qmcogg",
        "bkcmp3", "bkcflac", "tkm",
        "666c6163", "6d7033", "6f6767", "6d3461", "776177"
    )

    private val EXT_FALLBACK = mapOf(
        "mgg" to "ogg", "mgg0" to "ogg", "mgg1" to "ogg", "mggl" to "ogg",
        "mgg2" to "flac",
        "mflac" to "flac", "mflac0" to "flac", "mflac2" to "flac", "mflach" to "flac",
        "qmcflac" to "flac", "qmcogg" to "ogg",
        "qmc0" to "mp3", "qmc2" to "ogg", "qmc3" to "mp3",
        "bkcmp3" to "mp3", "bkcflac" to "flac", "tkm" to "m4a"
    )

    /** 双扩展名：xxx.mgg2.flac / xxx.mflac0.flac 等（真实扩展名在最后） */
    private val DOUBLE_EXT_REGEX = Regex(
        """\.(mgg\d*|mggl|mflac\d*|mflach|qmc\d+|qmcflac|qmcogg|bkcmp3|bkcflac|tkm)\.[a-z0-9]{2,5}$""",
        RegexOption.IGNORE_CASE
    )

    /** QQ音乐会员加密文件缺少内嵌密钥（STag/MusicEx 布局），无法离线解密 */
    class NeedsKeyException(val songId: String?, val songMid: String?) :
        Exception("该文件为 QQ音乐会员加密格式（STag），文件内不含解密密钥")

    class Result(
        val audio: ByteArray,
        val ext: String,
        val songId: Long?
    )

    fun isEncryptedFile(name: String): Boolean {
        if (name.substringAfterLast('.', "").lowercase() in ENCRYPTED_EXTENSIONS) return true
        return DOUBLE_EXT_REGEX.containsMatchIn(name)
    }

    /** 文件名中的加密格式扩展名（双扩展名时取中间那段） */
    private fun encryptedExtOf(name: String): String {
        val m = DOUBLE_EXT_REGEX.find(name)
        if (m != null) return m.groupValues[1].lowercase()
        return name.substringAfterLast('.', "").lowercase()
    }

    /** STag 尾部布局探测结果：songId + songMid（用于后续获取 ekey） */
    data class STagInfo(val songId: String?, val songMid: String?)

    /** 只读文件尾部，探测是否为 STag 布局（不内嵌 ekey 的会员加密格式） */
    fun probeSTag(file: File): STagInfo? {
        runCatching {
            java.io.RandomAccessFile(file, "r").use { raf ->
                val size = raf.length()
                if (size < 16) return null
                val tail = ByteArray(8)
                raf.seek(size - 8)
                raf.readFully(tail)
                if (String(tail, 4, 4, Charsets.US_ASCII) != "STag") return null
                val keySize = TeaCipher.readU32BE(tail, 0)
                if (keySize !in 1..0x300 || size - 8 - keySize < 0) return STagInfo(null, null)
                val meta = ByteArray(keySize)
                raf.seek(size - 8 - keySize)
                raf.readFully(meta)
                // 形如 "293430,2,0045NdiV2et0w0" -> songId, 版本, songMid
                val parts = String(meta, Charsets.US_ASCII).split(',')
                return STagInfo(
                    songId = parts.getOrNull(0)?.takeIf { it.isNotBlank() },
                    songMid = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                )
            }
        }
        return null
    }

    fun decrypt(file: File): Result {
        probeSTag(file)?.let { throw NeedsKeyException(it.songId, it.songMid) }
        return decrypt(file.readBytes(), encryptedExtOf(file.name))
    }

    fun decrypt(file: ByteArray, rawExt: String): Result {
        val size = file.size
        require(size > 16) { "file too small" }

        val audioSize: Int
        val cipher: QmcStreamCipher
        var songId: Long? = null

        val tail4 = String(file, size - 4, 4, Charsets.US_ASCII)
        if (tail4 == "QTag") {
            // [音频][ekey,songId,ver][u32 BE 长度]["QTag"]
            val keySize = readU32BE(file, size - 8)
            require(keySize in 1..(size - 8)) { "invalid QTag key size" }
            audioSize = size - keySize - 8
            val rawKey = file.copyOfRange(audioSize, size - 8)
            val keyEnd = rawKey.indexOf(','.code.toByte())
            require(keyEnd >= 0) { "invalid key: raw key not found" }
            cipher = cipherFor(QmcKey.derive(rawKey.copyOf(keyEnd)))
            val idBuf = rawKey.copyOfRange(keyEnd + 1, rawKey.size)
            val idEnd = idBuf.indexOf(','.code.toByte())
            if (idEnd > 0) {
                songId = String(idBuf, 0, idEnd, Charsets.US_ASCII).toLongOrNull()
            }
        } else {
            // [音频][原始 key][u32 LE 密钥长度]；长度异常时按 QMCv1 静态表处理
            val keySize = readU32LE(file, size - 4)
            if (keySize in 1 until 0x300 && keySize + 4 < size) {
                audioSize = size - keySize - 4
                val rawKey = file.copyOfRange(audioSize, size - 4)
                cipher = cipherFor(QmcKey.derive(rawKey))
            } else {
                audioSize = size
                cipher = QmcStaticCipher()
            }
        }

        require(audioSize > 0) { "invalid audio size" }
        val audio = file.copyOf(audioSize)
        cipher.decrypt(audio, 0)
        val ext = sniffAudioExt(audio, EXT_FALLBACK[rawExt] ?: "mp3")
        return Result(audio, ext, songId)
    }

    private fun cipherFor(key: ByteArray): QmcStreamCipher =
        if (key.size > 300) QmcRC4Cipher(key) else QmcMapCipher(key)

    /** 根据魔数嗅探真实音频格式 */
    fun sniffAudioExt(buf: ByteArray, fallback: String): String {
        if (buf.size < 12) return fallback
        fun ascii(off: Int, len: Int) = String(buf, off, len, Charsets.US_ASCII)
        return when {
            ascii(0, 4) == "fLaC" -> "flac"
            ascii(0, 4) == "OggS" -> "ogg"
            ascii(0, 3) == "ID3" -> "mp3"
            ascii(0, 4) == "RIFF" && ascii(8, 4) == "WAVE" -> "wav"
            ascii(4, 4) == "ftyp" -> "m4a"
            (buf[0].toInt() and 0xff) == 0xFF && (buf[1].toInt() and 0xE0) == 0xE0 -> "mp3"
            else -> fallback
        }
    }

    private fun readU32BE(b: ByteArray, off: Int): Int = TeaCipher.readU32BE(b, off)

    private fun readU32LE(b: ByteArray, off: Int): Int =
        (b[off].toInt() and 0xff) or
            ((b[off + 1].toInt() and 0xff) shl 8) or
            ((b[off + 2].toInt() and 0xff) shl 16) or
            ((b[off + 3].toInt() and 0xff) shl 24)
}
