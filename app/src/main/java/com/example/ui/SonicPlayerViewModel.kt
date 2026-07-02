package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.data.model.VideoEntity
import com.example.data.repository.MediaRepository
import com.example.playback.PlaybackManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SonicPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MediaRepository
    val playbackManager = PlaybackManager.getInstance(application)

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Library Data
    val allSongs: StateFlow<List<SongEntity>>
    val recentlyAddedSongs: StateFlow<List<SongEntity>>
    val favoriteSongs: StateFlow<List<SongEntity>>
    val recentlyPlayedSongs: StateFlow<List<SongEntity>>
    val playlists: StateFlow<List<PlaylistEntity>>
    val allVideos: StateFlow<List<VideoEntity>>
    val recentlyAddedVideos: StateFlow<List<VideoEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = MediaRepository(application, db.mediaDao())

        viewModelScope.launch {
            repository.prepopulateWithDemoStreams()
        }

        allSongs = repository.allSongs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recentlyAddedSongs = repository.recentlyAddedSongs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        favoriteSongs = repository.favoriteSongs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recentlyPlayedSongs = repository.recentlyPlayedSongs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        playlists = repository.playlists
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allVideos = repository.allVideos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recentlyAddedVideos = repository.recentlyAddedVideos
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun scanStorage() {
        viewModelScope.launch {
            _isScanning.value = true
            repository.scanStorage()
            _isScanning.value = false
        }
    }

    fun toggleFavorite(song: SongEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, !song.isFavorite)
        }
    }

    fun recordSongPlayback(song: SongEntity) {
        viewModelScope.launch {
            repository.recordPlayback(song.id)
        }
    }

    fun recordVideoPlayback(video: VideoEntity) {
        viewModelScope.launch {
            repository.recordVideoPlayback(video)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.addPlaylist(name)
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return repository.getSongsForPlaylist(playlistId)
    }

    fun deleteSong(song: SongEntity) {
        viewModelScope.launch {
            repository.deleteSong(song)
        }
    }

    fun deleteVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
        }
    }

    fun simulateFileCopy() {
        viewModelScope.launch {
            val randomNum = (100..999).random()
            val newSong = SongEntity(
                title = "Downloaded Beats #$randomNum",
                artist = "Sonic Beats Inc",
                album = "Shared Downloads",
                duration = 184000,
                uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                isLocal = true,
                dateAdded = System.currentTimeMillis()
            )
            repository.insertSong(newSong)
        }
    }

    fun simulateVideoCopy() {
        viewModelScope.launch {
            val randomNum = (100..999).random()
            val newVideo = VideoEntity(
                title = "Copied Video Clip #$randomNum",
                duration = 240000,
                uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
                size = 8500000,
                isLocal = true,
                dateAdded = System.currentTimeMillis()
            )
            repository.insertVideo(newVideo)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SonicPlayerViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SonicPlayerViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
