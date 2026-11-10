package com.landmusic.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.landmusic.app.data.Song
import com.landmusic.app.playback.PlayerManager
import com.landmusic.app.ui.components.ArtworkImage
import com.landmusic.app.ui.components.SongRow

/** 主页：大标题 + 播放全部/随机播放 + 最近添加 + 加密文件提示 */
@Composable
fun HomeScreen(
    viewModel: LibraryViewModel,
    playerManager: PlayerManager,
    hasAllFilesAccess: Boolean,
    onGrantAllFiles: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val songs by viewModel.songs.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item {
            Text(
                text = "主页",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "共 ${songs.size} 首" +
                    if (viewModel.encryptedCount > 0) " · 含 ${viewModel.encryptedCount} 首已解锁加密歌曲" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
            Spacer(Modifier.height(12.dp))

            // 播放 / 随机播放 胶囊按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { playerManager.playQueue(songs, 0, shuffle = false); onOpenPlayer() },
                    modifier = Modifier.weight(1f),
                    enabled = songs.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("播放", style = MaterialTheme.typography.labelLarge)
                }
                Button(
                    onClick = { playerManager.playQueue(songs, 0, shuffle = true); onOpenPlayer() },
                    modifier = Modifier.weight(1f),
                    enabled = songs.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("随机播放", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // 未授予「所有文件访问」时的提示卡
        if (!hasAllFilesAccess) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("解锁 QQ音乐加密文件", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "授予「所有文件访问」后，可扫描并播放 qqmusic/song 中的 .mflac/.mgg/.qmc 文件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onGrantAllFiles) { Text("去开启") }
                    }
                }
            }
        }

        if (loading && songs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
        } else if (songs.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("没有发现本地音乐", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "把音频文件放进 Music 目录，或授予所有文件访问以扫描 QQ音乐下载",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // 最近添加
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
                ) {
                    Text("最近添加", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "重新扫描",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("重新扫描", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                val recent = songs.take(20)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(recent, key = { it.id }) { song ->
                        RecentCard(song = song) {
                            playerManager.playSingle(song, songs)
                            onOpenPlayer()
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 全部歌曲（前 10 首预览）
            item {
                Text(
                    "全部歌曲",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
                )
            }
            items(songs.take(10), key = { "home_${it.id}" }) { song ->
                SongRow(song = song, onClick = {
                    playerManager.playSingle(song, songs)
                    onOpenPlayer()
                })
            }
        }
    }
}

@Composable
private fun RecentCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .padding(bottom = 4.dp)
            .clickable(onClick = onClick),
    ) {
        ArtworkImage(
            song = song,
            size = 140.dp,
            corner = 10.dp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
