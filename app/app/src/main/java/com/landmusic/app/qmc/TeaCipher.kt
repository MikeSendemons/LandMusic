package com.landmusic.app.qmc

/**
 * 标准 TEA 分组密码（big-endian，块 8 字节，密钥 16 字节）。
 * 移植自 golang.org/x/crypto/tea（unlock-music 的 utils/tea.ts 同构）。
 */
internal class TeaCipher(key: ByteArray, private val rounds: Int = 64) {

    init {
        require(key.size == KEY_SIZE) { "incorrect key size" }
        require(rounds % 2 == 0) { "odd number of rounds specified" }
    }

    private val k0 = readU32BE(key, 0)
    private val k1 = readU32BE(key, 4)
    private val k2 = readU32BE(key, 8)
    private val k3 = readU32BE(key, 12)

    fun decrypt(dst: ByteArray, dstOff: Int, src: ByteArray, srcOff: Int) {
        var v0 = readU32BE(src, srcOff)
        var v1 = readU32BE(src, srcOff + 4)
        var sum = DELTA * (rounds / 2)
        repeat(rounds / 2) {
            v1 -= ((v0 shl 4) + k2) xor (v0 + sum) xor ((v0 ushr 5) + k3)
            v0 -= ((v1 shl 4) + k0) xor (v1 + sum) xor ((v1 ushr 5) + k1)
            sum -= DELTA
        }
        writeU32BE(dst, dstOff, v0)
        writeU32BE(dst, dstOff + 4, v1)
    }

    companion object {
        const val BLOCK_SIZE = 8
        const val KEY_SIZE = 16
        const val DELTA = 0x9e3779b9.toInt()

        internal fun readU32BE(b: ByteArray, off: Int): Int =
            ((b[off].toInt() and 0xff) shl 24) or
                ((b[off + 1].toInt() and 0xff) shl 16) or
                ((b[off + 2].toInt() and 0xff) shl 8) or
                (b[off + 3].toInt() and 0xff)

        internal fun writeU32BE(b: ByteArray, off: Int, v: Int) {
            b[off] = (v ushr 24).toByte()
            b[off + 1] = (v ushr 16).toByte()
            b[off + 2] = (v ushr 8).toByte()
            b[off + 3] = v.toByte()
        }
    }
}
