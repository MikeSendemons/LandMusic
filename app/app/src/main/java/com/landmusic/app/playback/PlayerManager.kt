package com.landmusic.app.playback

import android.app.Application
import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.landmusic.app.data.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** 睡眠定时状态 */
sealed interface SleepTimer {
    data object Off : SleepTimer
    data class Countdown(val endsAtMs: Long) : SleepTimer
    data object EndOfTrack : SleepTimer
}

/**
 * UI 与 MediaController 之间的桥：维护播放队列、状态流、进度 ticker、睡眠定时。
 */
class PlayerManager(private val app: Application) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var controller: MediaController? = null
    private var songMap: Map<String, Song> = emptyMap()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffleOn = MutableStateFlow(false)
    val shuffleOn: StateFlow<Boolean> = _shuffleOn.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _sleepTimer = MutableStateFlow<SleepTimer>(SleepTimer.Off)
    val sleepTimer: StateFlow<SleepTimer> = _sleepTimer.asStateFlow()

    /** 倒计时剩余秒数（Countdown 模式下每秒更新） */
    private val _sleepRemainingSec = MutableStateFlow(0L)
    val sleepRemainingSec: StateFlow<Long> = _sleepRemainingSec.asStateFlow()

    private var positionJob: Job? = null
    private var sleepJob: Job? = null
    private var tickerStarted = false

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_sleepTimer.value == SleepTimer.EndOfTrack && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                cancelSleepTimer()
                controller?.pause()
            }
            updateCurrent(mediaItem)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _shuffleOn.value = shuffleModeEnabled
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && _sleepTimer.value == SleepTimer.EndOfTrack) {
                cancelSleepTimer()
                controller?.pause()
            }
            updateDuration()
        }
    }

    fun ensureConnected(onReady: (() -> Unit)? = null) {
        controller?.let {
            onReady?.invoke()
            return
        }
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            runCatching {
                val c = future.get()
                controller = c
                c.addListener(playerListener)
                _shuffleOn.value = c.shuffleModeEnabled
                _repeatMode.value = c.repeatMode
                updateCurrent(c.currentMediaItem)
                startTicker()
                onReady?.invoke()
            }
        }, MoreExecutors.directExecutor())
    }

    /** @return true 表示已启动播放（UI 可跳转播放页）；false 表示被拦截 */
    fun playQueue(songs: List<Song>, startIndex: Int, shuffle: Boolean = false): Boolean {
        if (songs.isEmpty()) return false
        val target = songs.getOrNull(startIndex)
        if (target?.needsKey == true) {
            toastNeedsKey(target)
            return false
        }
        // 缺少密钥的会员加密文件无法播放，从队列中剔除
        val playable = songs.filter { !it.needsKey }
        if (playable.isEmpty()) return false
        val newIndex = if (target != null) playable.indexOfFirst { it.id == target.id }.coerceAtLeast(0) else 0
        if (playable.size < songs.size) {
            android.widget.Toast.makeText(
                app, "已跳过 ${songs.size - playable.size} 首缺少密钥的会员加密歌曲", android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        ensureConnected {
            val c = controller ?: return@ensureConnected
            songMap = playable.associateBy { it.id }
            val items = playable.map { mediaItemFor(it) }
            c.shuffleModeEnabled = shuffle
            c.setMediaItems(items, newIndex.coerceIn(0, playable.lastIndex), 0L)
            c.prepare()
            c.play()
        }
        return true
    }

    private fun toastNeedsKey(song: Song) {
        android.widget.Toast.makeText(
            app,
            "《${song.title}》是 QQ音乐会员加密文件，本机缺少解密密钥，暂时无法播放",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun playSingle(song: Song, queue: List<Song>): Boolean {
        val idx = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        return playQueue(if (queue.isEmpty()) listOf(song) else queue, idx)
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.let {
        if (it.currentPosition > 3000) it.seekTo(0) else it.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) = controller?.seekTo(ms)

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    // ---------- 睡眠定时 ----------

    fun setSleepTimerMinutes(minutes: Int) {
        sleepJob?.cancel()
        val endsAt = System.currentTimeMillis() + minutes * 60_000L
        _sleepTimer.value = SleepTimer.Countdown(endsAt)
        sleepJob = scope.launch {
            while (isActive) {
                val remain = (endsAt - System.currentTimeMillis()) / 1000
                _sleepRemainingSec.value = remain.coerceAtLeast(0)
                if (remain <= 0) break
                delay(1000)
            }
            fadeOutAndPause()
            cancelSleepTimer()
        }
    }

    fun setSleepTimerEndOfTrack() {
        sleepJob?.cancel()
        _sleepTimer.value = SleepTimer.EndOfTrack
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepTimer.value = SleepTimer.Off
        _sleepRemainingSec.value = 0
    }

    private suspend fun fadeOutAndPause() {
        val c = controller ?: return
        val original = c.volume
        val steps = 12
        repeat(steps) {
            c.volume = original * (1f - (it + 1).toFloat() / steps)
            delay(500)
        }
        c.pause()
        c.volume = original
    }

    // ---------- 内部 ----------

    private fun mediaItemFor(song: Song): MediaItem {
        val uri = when {
            song.isEncrypted -> QmcUriResolver.uriFor(song.path)
            song.mediaUri != null -> Uri.parse(song.mediaUri)
            else -> Uri.fromFile(File(song.path))
        }
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build()
            )
            .build()
    }

    private fun updateCurrent(mediaItem: MediaItem?) {
        _currentSong.value = mediaItem?.mediaId?.let { songMap[it] }
        updateDuration()
    }

    private fun updateDuration() {
        val c = controller ?: return
        val d = c.duration
        _durationMs.value = if (d > 0) d else (_currentSong.value?.durationMs ?: 0L)
    }

    private fun startTicker() {
        if (tickerStarted) return
        tickerStarted = true
        positionJob = scope.launch {
            while (isActive) {
                val c = controller
                if (c != null) {
                    _positionMs.value = c.currentPosition.coerceAtLeast(0)
                    if (c.duration > 0) _durationMs.value = c.duration
                }
                delay(500)
            }
        }
    }
}
