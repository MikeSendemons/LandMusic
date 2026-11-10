package com.landmusic.app.qmc

/**
 * QMC 三种流密码：静态表（QMCv1）、Map（短密钥）、RC4 变体（长密钥）。
 * 移植自 unlock-music src/decrypt/qmc_cipher.ts。
 */
internal interface QmcStreamCipher {
    fun decrypt(buf: ByteArray, offset: Int)
}

/** QMCv1：固定 256 字节异或表 */
internal class QmcStaticCipher : QmcStreamCipher {

    override fun decrypt(buf: ByteArray, offset: Int) {
        for (i in buf.indices) {
            buf[i] = (buf[i].toInt() xor mask(offset + i)).toByte()
        }
    }

    private fun mask(off0: Int): Int {
        var offset = off0
        if (offset > 0x7fff) offset %= 0x7fff
        return STATIC_BOX[(offset * offset + 27) and 0xff].toInt() and 0xff
    }

    companion object {
        // prettier-ignore
        private val STATIC_BOX = byteArrayOf(
            0x77, 0x48, 0x32, 0x73, 0xDE.toByte(), 0xF2.toByte(), 0xC0.toByte(), 0xC8.toByte(), // 0x00
            0x95.toByte(), 0xEC.toByte(), 0x30, 0xB2.toByte(), 0x51, 0xC3.toByte(), 0xE1.toByte(), 0xA0.toByte(), // 0x08
            0x9E.toByte(), 0xE6.toByte(), 0x9D.toByte(), 0xCF.toByte(), 0xFA.toByte(), 0x7F, 0x14, 0xD1.toByte(), // 0x10
            0xCE.toByte(), 0xB8.toByte(), 0xDC.toByte(), 0xC3.toByte(), 0x4A, 0x67, 0x93.toByte(), 0xD6.toByte(), // 0x18
            0x28, 0xC2.toByte(), 0x91.toByte(), 0x70, 0xCA.toByte(), 0x8D.toByte(), 0xA2.toByte(), 0xA4.toByte(), // 0x20
            0xF0.toByte(), 0x08, 0x61, 0x90.toByte(), 0x7E, 0x6F, 0xA2.toByte(), 0xE0.toByte(), // 0x28
            0xEB.toByte(), 0xAE.toByte(), 0x3E, 0xB6.toByte(), 0x67, 0xC7.toByte(), 0x92.toByte(), 0xF4.toByte(), // 0x30
            0x91.toByte(), 0xB5.toByte(), 0xF6.toByte(), 0x6C, 0x5E, 0x84.toByte(), 0x40, 0xF7.toByte(), // 0x38
            0xF3.toByte(), 0x1B, 0x02, 0x7F, 0xD5.toByte(), 0xAB.toByte(), 0x41, 0x89.toByte(), // 0x40
            0x28, 0xF4.toByte(), 0x25, 0xCC.toByte(), 0x52, 0x11, 0xAD.toByte(), 0x43, // 0x48
            0x68, 0xA6.toByte(), 0x41, 0x8B.toByte(), 0x84.toByte(), 0xB5.toByte(), 0xFF.toByte(), 0x2C, // 0x50
            0x92.toByte(), 0x4A, 0x26, 0xD8.toByte(), 0x47, 0x6A, 0x7C, 0x95.toByte(), // 0x58
            0x61, 0xCC.toByte(), 0xE6.toByte(), 0xCB.toByte(), 0xBB.toByte(), 0x3F, 0x47, 0x58, // 0x60
            0x89.toByte(), 0x75, 0xC3.toByte(), 0x75, 0xA1.toByte(), 0xD9.toByte(), 0xAF.toByte(), 0xCC.toByte(), // 0x68
            0x08, 0x73, 0x17, 0xDC.toByte(), 0xAA.toByte(), 0x9A.toByte(), 0xA2.toByte(), 0x16, // 0x70
            0x41, 0xD8.toByte(), 0xA2.toByte(), 0x06, 0xC6.toByte(), 0x8B.toByte(), 0xFC.toByte(), 0x66, // 0x78
            0x34, 0x9F.toByte(), 0xCF.toByte(), 0x18, 0x23, 0xA0.toByte(), 0x0A, 0x74, // 0x80
            0xE7.toByte(), 0x2B, 0x27, 0x70, 0x92.toByte(), 0xE9.toByte(), 0xAF.toByte(), 0x37, // 0x88
            0xE6.toByte(), 0x8C.toByte(), 0xA7.toByte(), 0xBC.toByte(), 0x62, 0x65, 0x9C.toByte(), 0xC2.toByte(), // 0x90
            0x08, 0xC9.toByte(), 0x88.toByte(), 0xB3.toByte(), 0xF3.toByte(), 0x43, 0xAC.toByte(), 0x74, // 0x98
            0x2C, 0x0F, 0xD4.toByte(), 0xAF.toByte(), 0xA1.toByte(), 0xC3.toByte(), 0x01, 0x64, // 0xA0
            0x95.toByte(), 0x4E, 0x48, 0x9F.toByte(), 0xF4.toByte(), 0x35, 0x78, 0x95.toByte(), // 0xA8
            0x7A, 0x39, 0xD6.toByte(), 0x6A, 0xA0.toByte(), 0x6D, 0x40, 0xE8.toByte(), // 0xB0
            0x4F, 0xA8.toByte(), 0xEF.toByte(), 0x11, 0x1D, 0xF3.toByte(), 0x1B, 0x3F, // 0xB8
            0x3F, 0x07, 0xDD.toByte(), 0x6F, 0x5B, 0x19, 0x30, 0x19, // 0xC0
            0xFB.toByte(), 0xEF.toByte(), 0x0E, 0x37, 0xF0.toByte(), 0x0E, 0xCD.toByte(), 0x16, // 0xC8
            0x49, 0xFE.toByte(), 0x53, 0x47, 0x13, 0x1A, 0xBD.toByte(), 0xA4.toByte(), // 0xD0
            0xF1.toByte(), 0x40, 0x19, 0x60, 0x0E.toByte(), 0xED.toByte(), 0x68, 0x09, // 0xD8
            0x06, 0x5F, 0x4D, 0xCF.toByte(), 0x3D, 0x1A, 0xFE.toByte(), 0x20, // 0xE0
            0x77, 0xE4.toByte(), 0xD9.toByte(), 0xDA.toByte(), 0xF9.toByte(), 0xA4.toByte(), 0x2B, 0x76, // 0xE8
            0x1C, 0x71, 0xDB.toByte(), 0x00, 0xBC.toByte(), 0xFD.toByte(), 0x0C, 0x6C, // 0xF0
            0xA5.toByte(), 0x47, 0xF7.toByte(), 0xF6.toByte(), 0x00, 0x79, 0x4A, 0x11 // 0xF8
        )
    }
}

/** QMCv2 短密钥（<=300 字节）：Map 流密码 */
internal class QmcMapCipher(private val key: ByteArray) : QmcStreamCipher {

    init {
        require(key.isNotEmpty()) { "qmc/cipher_map: invalid key size" }
    }

    private val n = key.size

    override fun decrypt(buf: ByteArray, offset: Int) {
        for (i in buf.indices) {
            buf[i] = (buf[i].toInt() xor mask(offset + i)).toByte()
        }
    }

    private fun mask(off0: Int): Int {
        var offset = off0
        if (offset > 0x7fff) offset %= 0x7fff
        val idx = (offset * offset + 71214) % n
        return rotate(key[idx].toInt() and 0xff, idx and 0x7)
    }

    private fun rotate(value: Int, bits: Int): Int {
        val r = (bits + 4) % 8
        return ((value shl r) or (value ushr r)) and 0xff
    }
}

/** QMCv2 长密钥（>300 字节）：RC4 变体，按 5120 字节分段重置 */
internal class QmcRC4Cipher(private val key: ByteArray) : QmcStreamCipher {

    init {
        require(key.isNotEmpty()) { "invalid key size" }
    }

    private val n = key.size
    private val s = ByteArray(n) { it.toByte() }
    private var hash: Long = 1

    init {
        var j = 0
        for (i in 0 until n) {
            j = ((s[i].toInt() and 0xff) + j + (key[i].toInt() and 0xff)) % n
            val t = s[i]; s[i] = s[j]; s[j] = t
        }
        for (i in 0 until n) {
            val value = key[i].toInt() and 0xff
            if (value == 0) continue
            val next = (hash * value) and 0xFFFF_FFFFL
            if (next == 0L || next <= hash) break
            hash = next
        }
    }

    override fun decrypt(buf: ByteArray, offset0: Int) {
        var offset = offset0
        var toProcess = buf.size
        var processed = 0

        fun postProcess(len: Int): Boolean {
            toProcess -= len
            processed += len
            offset += len
            return toProcess == 0
        }

        if (offset < FIRST_SEGMENT_SIZE) {
            val len = minOf(buf.size, FIRST_SEGMENT_SIZE - offset)
            encFirstSegment(buf, 0, len, offset)
            if (postProcess(len)) return
        }

        if (offset % SEGMENT_SIZE != 0) {
            val len = minOf(SEGMENT_SIZE - (offset % SEGMENT_SIZE), toProcess)
            encASegment(buf, processed, len, offset)
            if (postProcess(len)) return
        }

        while (toProcess > SEGMENT_SIZE) {
            encASegment(buf, processed, SEGMENT_SIZE, offset)
            postProcess(SEGMENT_SIZE)
        }

        if (toProcess > 0) {
            encASegment(buf, processed, toProcess, offset)
        }
    }

    private fun encFirstSegment(buf: ByteArray, start: Int, len: Int, offset: Int) {
        for (i in 0 until len) {
            buf[start + i] =
                (buf[start + i].toInt() xor (key[getSegmentKey(offset + i)].toInt() and 0xff)).toByte()
        }
    }

    private fun encASegment(buf: ByteArray, start: Int, len: Int, offset: Int) {
        val box = s.copyOf()
        val skipLen = (offset % SEGMENT_SIZE) + getSegmentKey(offset / SEGMENT_SIZE)
        var j = 0
        var k = 0
        var i = -skipLen
        while (i < len) {
            j = (j + 1) % n
            k = ((box[j].toInt() and 0xff) + k) % n
            val t = box[k]; box[k] = box[j]; box[j] = t
            if (i >= 0) {
                val m = ((box[j].toInt() and 0xff) + (box[k].toInt() and 0xff)) % n
                buf[start + i] = (buf[start + i].toInt() xor (box[m].toInt() and 0xff)).toByte()
            }
            i++
        }
    }

    private fun getSegmentKey(id: Int): Int {
        val seed = key[id % n].toInt() and 0xff
        val idx = ((hash.toDouble() / ((id + 1).toDouble() * seed)) * 100.0).toLong()
        return (idx % n).toInt()
    }

    companion object {
        private const val FIRST_SEGMENT_SIZE = 0x80
        private const val SEGMENT_SIZE = 5120
    }
}
