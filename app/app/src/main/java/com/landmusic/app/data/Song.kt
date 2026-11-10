package com.landmusic.app.data

/** 本地歌曲条目：标准音频（MediaStore）或 QQ音乐加密文件 */
data class Song(
    val id: String,          // 稳定唯一键（ms_<id> 或 qmc_<pathHash>）
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val path: String,        // 文件绝对路径
    val mediaUri: String?,   // MediaStore content uri（标准音频）
    val isEncrypted: Boolean,
    val dateAddedSec: Long,
    val needsKey: Boolean = false,   // STag 会员加密：文件不含密钥，无法离线解密
    val songMid: String? = null,     // QQ音乐 songMid（用于后续获取 ekey）
) {
    val isValid: Boolean get() = title.isNotBlank()
}
