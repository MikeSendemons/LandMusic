package com.landmusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.landmusic.app.data.Song

/** 歌曲封面：内嵌图片，失败时显示灰色音符占位 */
@Composable
fun ArtworkImage(
    song: Song,
    size: Dp? = null,
    corner: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(corner)
    SubcomposeAsyncImage(
        model = song,
        contentDescription = song.title,
        modifier = modifier
            .then(if (size != null) Modifier.size(size) else Modifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentScale = ContentScale.Crop,
        error = { ArtworkPlaceholder(size ?: 48.dp) },
        loading = { ArtworkPlaceholder(size ?: 48.dp) },
    )
}

@Composable
private fun ArtworkPlaceholder(size: Dp) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(size * 0.45f),
        )
    }
}
