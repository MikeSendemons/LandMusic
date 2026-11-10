package com.landmusic.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.landmusic.app.data.MediaScanner
import com.landmusic.app.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = MediaScanner(app)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val standardCount: Int get() = _songs.value.count { !it.isEncrypted }
    val encryptedCount: Int get() = _songs.value.count { it.isEncrypted && !it.needsKey }
    val needsKeyCount: Int get() = _songs.value.count { it.needsKey }

    fun refresh() {
        if (_loading.value) return
        viewModelScope.launch {
            _loading.value = true
            val result = withContext(Dispatchers.IO) { scanner.scanAll() }
            _songs.value = result
            _loading.value = false
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun searchResults(): List<Song> {
        val q = _query.value.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return _songs.value.filter {
            it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
        }
    }

    fun albums(): Map<String, List<Song>> =
        _songs.value.groupBy { "${it.album}|${it.artist}" }
}
