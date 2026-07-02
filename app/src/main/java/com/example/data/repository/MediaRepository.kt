package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.dao.MediaDao
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MediaRepository(
    private val context: Context,
    private val mediaDao: MediaDao
) {
    val allSongs: Flow<List<SongEntity>> = mediaDao.getAllSongs()
    val recentlyAddedSongs: Flow<List<SongEntity>> = mediaDao.getRecentlyAddedSongs()
    val favoriteSongs: Flow<List<SongEntity>> = mediaDao.getFavoriteSongs()
    val recentlyPlayedSongs: Flow<List<SongEntity>> = mediaDao.getRecentlyPlayedSongs()

    val allVideos: Flow<List<VideoEntity>> = mediaDao.getAllVideos()
    val recentlyAddedVideos: Flow<List<VideoEntity>> = mediaDao.getRecentlyAddedVideos()

    val playlists: Flow<List<PlaylistEntity>> = mediaDao.getAllPlaylists()

    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return mediaDao.getSongsForPlaylist(playlistId)
    }

    suspend fun toggleFavorite(id: Long, isFavorite: Boolean) {
        mediaDao.toggleFavorite(id, isFavorite)
    }

    suspend fun recordPlayback(id: Long) {
        mediaDao.incrementSongPlayCount(id)
    }

    suspend fun recordVideoPlayback(video: VideoEntity) {
        val updated = video.copy(
            playCount = video.playCount + 1,
            lastPlayedAt = System.currentTimeMillis()
        )
        mediaDao.updateVideo(updated)
    }

    suspend fun addPlaylist(name: String): Long {
        return mediaDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        mediaDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        mediaDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        mediaDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun insertSong(song: SongEntity): Long {
        return mediaDao.insertSong(song)
    }

    suspend fun insertVideo(video: VideoEntity): Long {
        return mediaDao.insertVideo(video)
    }

    suspend fun deleteSong(song: SongEntity) {
        mediaDao.deleteSong(song)
    }

    suspend fun deleteVideo(video: VideoEntity) {
        mediaDao.deleteVideo(video)
    }

    suspend fun prepopulateWithDemoStreams() = withContext(Dispatchers.IO) {
        val currentSongs = allSongs.first()
        val currentVideos = allVideos.first()
        
        if (currentSongs.isEmpty()) {
            val demoSongs = listOf(
                SongEntity(
                    title = "Neon Horizon",
                    artist = "Sonic Wave Corporation",
                    album = "Neon Horizon LP",
                    duration = 302000,
                    uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    isLocal = false,
                    dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 3 // 3 days ago
                ),
                SongEntity(
                    title = "Deep Space Echoes",
                    artist = "Echo Nebula",
                    album = "Interstellar Suite",
                    duration = 423000,
                    uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    isLocal = false,
                    dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2 // 2 days ago
                ),
                SongEntity(
                    title = "Midnight Cyber Drive",
                    artist = "Tokyo Outrun",
                    album = "Metropolis Pulse",
                    duration = 344000,
                    uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    isLocal = false,
                    dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 12 // 12 hours ago
                ),
                SongEntity(
                    title = "Chill Lofi Afternoon",
                    artist = "Summer Dreamer",
                    album = "Lofi Study Beats",
                    duration = 302000,
                    uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    isLocal = false,
                    dateAdded = System.currentTimeMillis() // Just added
                ),
                SongEntity(
                    title = "Electric Horizon",
                    artist = "Wave Rider",
                    album = "Classic Outrun",
                    duration = 362000,
                    uriString = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    isLocal = false,
                    dateAdded = System.currentTimeMillis()
                )
            )
            mediaDao.insertSongs(demoSongs)
        }

        if (currentVideos.isEmpty()) {
            val demoVideos = listOf(
                VideoEntity(
                    title = "Big Buck Bunny (Animation HD)",
                    duration = 596000,
                    uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    size = 10800000,
                    isLocal = false,
                    dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 5
                ),
                VideoEntity(
                    title = "Sintel (Fantasy Movie HD)",
                    duration = 652000,
                    uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
                    size = 14500000,
                    isLocal = false,
                    dateAdded = System.currentTimeMillis() - 1000 * 60 * 60 * 2
                ),
                VideoEntity(
                    title = "Tears of Steel (Sci-Fi Visuals)",
                    duration = 734000,
                    uriString = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    size = 22100000,
                    isLocal = false,
                    dateAdded = System.currentTimeMillis()
                )
            )
            mediaDao.insertVideos(demoVideos)
        }
    }

    suspend fun scanStorage() = withContext(Dispatchers.IO) {
        try {
            val songsList = mutableListOf<SongEntity>()
            val videosList = mutableListOf<VideoEntity>()

            // Query Audio
            val audioProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED
            )

            val audioCursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                audioProjection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                null
            )

            audioCursor?.use { cursor ->
                val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: "Unknown" else "Unknown"
                    val artist = if (artistCol != -1) cursor.getString(artistCol) ?: "Unknown Artist" else "Unknown Artist"
                    val album = if (albumCol != -1) cursor.getString(albumCol) ?: "Unknown Album" else "Unknown Album"
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val data = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) * 1000 else System.currentTimeMillis()

                    songsList.add(
                        SongEntity(
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            uriString = data,
                            isLocal = true,
                            dateAdded = dateAdded
                        )
                    )
                }
            }

            // Query Video
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED
            )

            val videoCursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                null
            )

            videoCursor?.use { cursor ->
                val titleCol = cursor.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val title = if (titleCol != -1) cursor.getString(titleCol) ?: "Unknown Video" else "Unknown Video"
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L
                    val data = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L
                    val dateAdded = if (dateAddedCol != -1) cursor.getLong(dateAddedCol) * 1000 else System.currentTimeMillis()

                    videosList.add(
                        VideoEntity(
                            title = title,
                            duration = duration,
                            uriString = data,
                            size = size,
                            isLocal = true,
                            dateAdded = dateAdded
                        )
                    )
                }
            }

            if (songsList.isNotEmpty()) {
                // To keep database simple and avoid duplicates on repeat scans:
                // We'll insert and rely on Room's OnConflictStrategy.REPLACE or custom logic.
                mediaDao.insertSongs(songsList)
            }
            if (videosList.isNotEmpty()) {
                mediaDao.insertVideos(videosList)
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error scanning media", e)
        }
    }
}
