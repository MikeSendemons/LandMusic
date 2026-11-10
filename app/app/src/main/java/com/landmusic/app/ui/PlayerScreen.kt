package com.landmusic.app.ui

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.landmusic.app.data.Song
import com.landmusic.app.playback.PlayerManager
import com.landmusic.app.playback.SleepTimer
import com.landmusic.app.ui.components.ArtworkImage
import com.landmusic.app.ui.components.formatDuration
import com.landmusic.app.ui.theme.LandRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 全屏播放页：封面取色渐变背景 + 控制 + 睡眠定时 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerManager: PlayerManager,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val song by playerManager.currentSong.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val position by playerManager.positionMs.collectAsState()
    val duration by playerManager.durationMs.collectAsState()
    val shuffleOn by playerManager.shuffleOn.collectAsState()
    val repeatMode by playerManager.repeatMode.collectAsState()
    val sleepTimer by playerManager.sleepTimer.collectAsState()
    val sleepRemaining by playerManager.sleepRemainingSec.collectAsState()

    val current = song
    var showTimerSheet by remember { mutableStateOf(false) }

    // 封面取色（Palette）驱动背景渐变
    val context = LocalContext.current
    var paletteColor by remember(current?.id) { mutableStateOf<Color?>(null) }
    LaunchedEffect(current?.id) {
        val s = current ?: return@LaunchedEffect
        paletteColor = loadPaletteColor(context, s)
    }

    val topColor = paletteColor ?: Color(0xFF3A3A3C)
    val darkTheme = topColor.luminance() < 0.5f
    val contentColor = Color.White
    val secondaryContent = Color.White.copy(alpha = 0.7f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        topColor.copy(alpha = 0.95f),
                        topColor.copy(alpha = 0.6f),
                        Color(0xFF161617),
                    )
                )
            ),
    ) {
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有正在播放的歌曲", color = secondaryContent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 顶部条
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            Icons.Rounded.ExpandMore,
                            contentDescription = "收起",
                            tint = contentColor,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "正在播放",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryContent,
                    )
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(48.dp))
                }

                Spacer(Modifier.weight(1f))

                // 封面
                ArtworkImage(
                    song = current,
                    size = 320.dp,
                    corner = 14.dp,
                    modifier = Modifier
                        .shadow(24.dp, RoundedCornerShape(14.dp)),
                )

                Spacer(Modifier.weight(1f))

                // 标题 / 艺人
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = current.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (darkTheme) Color(0xFFFF8A9B) else LandRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (current.isEncrypted) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "QQ音乐加密格式 · 已解锁播放",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContent,
                    )
                }

                Spacer(Modifier.height(18.dp))

                // 进度条
                val dur = duration.coerceAtLeast(current.durationMs)
                Slider(
                    value = if (dur > 0) position.toFloat().coerceIn(0f, dur.toFloat()) else 0f,
                    onValueChange = { playerManager.seekTo(it.toLong()) },
                    valueRange = 0f..(if (dur > 0) dur.toFloat() else 1f),
                    colors = SliderDefaults.colors(
                        thumbColor = contentColor,
                        activeTrackColor = contentColor,
                        inactiveTrackColor = contentColor.copy(alpha = 0.3f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        formatDuration(position),
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContent,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "-${formatDuration((dur - position).coerceAtLeast(0))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryContent,
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 控制按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { playerManager.toggleShuffle() }) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "随机播放",
                            tint = if (shuffleOn) contentColor else contentColor.copy(alpha = 0.45f),
                        )
                    }
                    IconButton(onClick = { playerManager.previous() }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "上一首",
                            tint = contentColor,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(contentColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(onClick = { playerManager.togglePlayPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = topColor,
                                modifier = Modifier.size(38.dp),
                            )
                        }
                    }
                    IconButton(onClick = { playerManager.next() }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "下一首",
                            tint = contentColor,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    IconButton(onClick = { playerManager.cycleRepeat() }) {
                        Icon(
                            imageVector = when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "循环模式",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) contentColor
                            else contentColor.copy(alpha = 0.45f),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 睡眠定时入口
                TextButton(onClick = { showTimerSheet = true }) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (val t = sleepTimer) {
                            is SleepTimer.Countdown -> "定时停止 ${formatDuration(sleepRemaining * 1000)}"
                            SleepTimer.EndOfTrack -> "播完当前歌曲停止"
                            SleepTimer.Off -> "定时停止播放"
                        },
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }

        if (showTimerSheet) {
            ModalBottomSheet(onDismissRequest = { showTimerSheet = false }) {
                SleepTimerSheet(
                    playerManager = playerManager,
                    onDone = { showTimerSheet = false },
                )
            }
        }
    }
}

@Composable
private fun SleepTimerSheet(
    playerManager: PlayerManager,
    onDone: () -> Unit,
) {
    val sleepTimer by playerManager.sleepTimer.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Text("定时停止播放", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(12.dp))

        val options = listOf(5, 10, 15, 30, 45, 60)
        options.forEach { minutes ->
            TextButton(
                onClick = {
                    playerManager.setSleepTimerMinutes(minutes)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "$minutes 分钟后停止",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        TextButton(
            onClick = {
                playerManager.setSleepTimerEndOfTrack()
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "播完当前歌曲后停止",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (sleepTimer != SleepTimer.Off) {
            TextButton(
                onClick = {
                    playerManager.cancelSleepTimer()
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "关闭定时",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 用 Coil 单例（含自定义封面 Fetcher）取 bitmap，再经 Palette 提取主色 */
private suspend fun loadPaletteColor(context: android.content.Context, song: Song): Color? =
    withContext(Dispatchers.IO) {
        runCatching {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(song)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    ?: return@runCatching null
                val palette = Palette.from(bitmap).generate()
                val swatch = palette.vibrantSwatch
                    ?: palette.darkVibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                swatch?.rgb?.let { Color(it) }
            } else null
        }.getOrNull()
    }
