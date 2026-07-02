package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.SongEntity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(UnstableApi::class)
class PlaybackManager private constructor(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? = null
        private set

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<SongEntity>>(emptyList())
    val currentQueue: StateFlow<List<SongEntity>> = _currentQueue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private var positionPoller: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                mediaController = controller
                setupControllerListener(controller)
                Log.d("PlaybackManager", "MediaController initialized successfully")
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Failed to initialize MediaController", e)
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupControllerListener(controller: MediaController?) {
        controller?.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateStateFromController()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionPoller()
                } else {
                    stopPositionPoller()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateStateFromController()
            }
        })
        updateStateFromController()
    }

    private fun updateStateFromController() {
        val controller = mediaController ?: return
        _isPlaying.value = controller.isPlaying
        _duration.value = controller.duration.coerceAtLeast(0L)
        _currentPosition.value = controller.currentPosition.coerceAtLeast(0L)
        
        val currentMediaItem = controller.currentMediaItem
        if (currentMediaItem != null) {
            val songId = currentMediaItem.mediaId.toLongOrNull() ?: -1L
            val song = _currentQueue.value.find { it.id == songId }
                ?: SongEntity(
                    id = songId,
                    title = currentMediaItem.mediaMetadata.title?.toString() ?: "Unknown",
                    artist = currentMediaItem.mediaMetadata.artist?.toString() ?: "Unknown Artist",
                    album = currentMediaItem.mediaMetadata.albumTitle?.toString() ?: "Unknown Album",
                    duration = controller.duration.coerceAtLeast(0L),
                    uriString = currentMediaItem.requestMetadata.mediaUri?.toString() ?: ""
                )
            _currentSong.value = song
            _currentIndex.value = controller.currentMediaItemIndex
        } else {
            _currentSong.value = null
            _currentIndex.value = -1
        }
        
        if (controller.isPlaying) {
            startPositionPoller()
        } else {
            stopPositionPoller()
        }
    }

    private fun startPositionPoller() {
        stopPositionPoller()
        positionPoller = scope.launch {
            while (isActive) {
                mediaController?.let {
                    _currentPosition.value = it.currentPosition.coerceAtLeast(0L)
                    _duration.value = it.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun stopPositionPoller() {
        positionPoller?.cancel()
        positionPoller = null
    }

    fun playSong(song: SongEntity, queue: List<SongEntity>) {
        val controller = mediaController ?: return
        _currentQueue.value = queue
        
        val index = queue.indexOfFirst { it.id == song.id }
        
        val mediaItems = queue.map { item ->
            MediaItem.Builder()
                .setMediaId(item.id.toString())
                .setUri(item.uriString)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(item.title)
                        .setArtist(item.artist)
                        .setAlbumTitle(item.album)
                        .build()
                )
                .build()
        }

        controller.setMediaItems(mediaItems)
        val targetIndex = if (index != -1) index else 0
        controller.seekTo(targetIndex, 0L)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_IDLE) {
                controller.prepare()
            }
            controller.play()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    fun skipToNext() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNext()
        }
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.hasPreviousMediaItem()) {
            controller.seekToPrevious()
        }
    }

    fun release() {
        stopPositionPoller()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
        mediaController = null
    }

    companion object {
        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PlaybackManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
