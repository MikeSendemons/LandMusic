package com.landmusic.app.qmc

import java.util.Base64
import kotlin.math.abs
import kotlin.math.tan

/**
 * QQ音乐 ekey 派生：base64 解码后用腾讯 TEA（CBC + 自定义填充）还原真实密钥。
 * 移植自 unlock-music src/decrypt/qmc_key.ts（仅用于用户本人合法下载的文件）。
 */
internal object QmcKey {

    private const val SALT_LEN = 2
    private const val ZERO_LEN = 7

    fun derive(raw: ByteArray): ByteArray {
        val rawDec = Base64.getMimeDecoder().decode(String(raw, Charsets.US_ASCII))
        require(rawDec.size >= 16) { "key length is too short" }

        val simpleKey = simpleMakeKey(106, 8)
        val teaKey = ByteArray(16) { i ->
            if (i % 2 == 0) simpleKey[i / 2].toByte() else rawDec[i / 2]
        }
        val sub = decryptTencentTea(rawDec.copyOfRange(8, rawDec.size), teaKey)
        return rawDec.copyOf(8) + sub
    }

    internal fun simpleMakeKey(salt: Int, length: Int): IntArray =
        IntArray(length) { i -> ((abs(tan(salt + i * 0.1)) * 100.0).toInt()) and 0xff }

    /** 密文格式：PadLen(1) + Padding(0-7) + Salt(2) + Body + Zero(7) */
    private fun decryptTencentTea(inBuf: ByteArray, key: ByteArray): ByteArray {
        require(inBuf.size % 8 == 0) { "inBuf size not a multiple of the block size" }
        require(inBuf.size >= 16) { "inBuf size too small" }

        val blk = TeaCipher(key, 32)
        val tmp = ByteArray(8)
        blk.decrypt(tmp, 0, inBuf, 0)

        val nPadLen = tmp[0].toInt() and 0x7
        val outLen = inBuf.size - 1 - nPadLen - SALT_LEN - ZERO_LEN
        require(outLen >= 0) { "invalid padding" }
        val out = ByteArray(outLen)

        var ivPrev = ByteArray(8)
        var ivCur = inBuf.copyOfRange(0, 8)
        var inPos = 8
        var tmpIdx = 1 + nPadLen

        fun cryptBlock() {
            ivPrev = ivCur
            ivCur = inBuf.copyOfRange(inPos, inPos + 8)
            for (j in 0..7) tmp[j] = (tmp[j].toInt() xor ivCur[j].toInt()).toByte()
            blk.decrypt(tmp, 0, tmp, 0)
            inPos += 8
            tmpIdx = 0
        }

        // 跳过 Salt
        var i = 1
        while (i <= SALT_LEN) {
            if (tmpIdx < 8) {
                tmpIdx++
                i++
            } else {
                cryptBlock()
            }
        }

        // 还原明文
        var outPos = 0
        while (outPos < outLen) {
            if (tmpIdx < 8) {
                out[outPos] = (tmp[tmpIdx].toInt() xor ivPrev[tmpIdx].toInt()).toByte()
                outPos++
                tmpIdx++
            } else {
                cryptBlock()
            }
        }

        // 校验 Zero（与原实现一致：仅检查当前位置字节）
        repeat(ZERO_LEN) {
            if (tmpIdx >= 8 || tmp[tmpIdx] != ivPrev[tmpIdx]) {
                throw IllegalStateException("zero check failed")
            }
        }
        return out
    }
}
